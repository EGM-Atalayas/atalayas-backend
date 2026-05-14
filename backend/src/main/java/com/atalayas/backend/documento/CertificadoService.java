package com.atalayas.backend.documento;

import com.atalayas.backend.communication.service.NotificationService;
import com.atalayas.backend.content.repository.ContentRepository;
import com.atalayas.backend.documento.entity.Documento;
import com.atalayas.backend.documento.entity.DocumentoAsignacion;
import com.atalayas.backend.documento.enums.TipoDocumento;
import com.atalayas.backend.documento.repository.DocumentoAsignacionRepository;
import com.atalayas.backend.documento.repository.DocumentoRepository;
import com.atalayas.backend.module.entity.TrainingModule;
import com.atalayas.backend.module.repository.ModuleRepository;
import com.atalayas.backend.progress.repository.ProgressRepository;
import com.atalayas.backend.ai.service.SupabaseStorageService;
import com.atalayas.backend.user.entity.User;
import com.atalayas.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Genera automáticamente un certificado PDF cuando un empleado
 * completa todos los contenidos activos de un módulo formativo.
 *
 * Flujo:
 *  1. ProgressService llama a {@link #generarSiModuloCompletado} tras marcar
 *     un contenido como completado.
 *  2. Comprobamos si quedan contenidos pendientes en ese módulo para ese usuario.
 *  3. Si el módulo está al 100 %: generamos el PDF, lo subimos a Supabase,
 *     creamos un Documento (tipo=CERTIFICADO) y una DocumentoAsignacion.
 *  4. Enviamos notificación interna al empleado.
 *
 * La operación corre en una transacción nueva (REQUIRES_NEW) para no bloquear
 * la transacción principal de registro de progreso si algo falla.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CertificadoService {

    private final ProgressRepository        progressRepository;
    private final ContentRepository         contentRepository;
    private final ModuleRepository          moduleRepository;
    private final UserRepository            userRepository;
    private final DocumentoRepository       documentoRepository;
    private final DocumentoAsignacionRepository asignacionRepository;
    private final SupabaseStorageService    supabaseStorageService;
    private final NotificationService       notificationService;

    // ── PUNTOS DE ENTRADA ──────────────────────────────────────────────────

    /**
     * Llamado directamente por el frontend cuando el empleado termina el último
     * contenido del módulo (sin depender del conteo de trazabilidad_lectura).
     *
     * Obtiene el usuario del contexto de seguridad y delega en {@link #doGenerar}.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String solicitarCertificado(UUID moduloId) {
        var user     = com.atalayas.backend.common.util.SecurityUtils.getCurrentUser();
        UUID userId    = user.getUsuarioId();
        UUID empresaId = user.getEmpresaId();
        if (empresaId == null) throw new IllegalStateException("Usuario sin empresa asignada");
        return doGenerar(userId, moduloId, empresaId);
    }

    /**
     * Comprueba si el usuario ha completado todos los contenidos activos del módulo
     * y, en caso afirmativo, genera y persiste el certificado.
     * Se llama desde ProgressService vía afterCommit.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generarSiModuloCompletado(UUID usuarioId, UUID moduloId, UUID empresaId) {
        try {
            // 1. Cuántos contenidos activos tiene el módulo
            int totalContenidos = contentRepository
                    .findByModuloIdAndActivoTrueOrderByOrdenAsc(moduloId)
                    .size();
            if (totalContenidos == 0) return;

            // 2. Cuántos ha completado este usuario en ese módulo
            long completados = progressRepository
                    .countByUsuarioIdAndModuloIdAndCompletadoTrue(usuarioId, moduloId);
            if (completados < totalContenidos) return;

            doGenerar(usuarioId, moduloId, empresaId);

        } catch (Exception e) {
            log.error("Error generando certificado para usuario={} módulo={}: {}",
                    usuarioId, moduloId, e.getMessage(), e);
        }
    }

    /**
     * Lógica central: genera el PDF, lo sube a Supabase y crea el Documento.
     * Devuelve la URL del certificado generado (o del ya existente si ya había uno).
     */
    private String doGenerar(UUID usuarioId, UUID moduloId, UUID empresaId) {
        // Evitar duplicados
        boolean yaGenerado = asignacionRepository.existsCertificado(
                usuarioId, empresaId, TipoDocumento.CERTIFICADO.name(),
                buildDescripcionClave(moduloId));
        if (yaGenerado) {
            log.debug("Certificado ya existente para usuario={} módulo={}", usuarioId, moduloId);
            return asignacionRepository.findCertificadoUrl(usuarioId, buildDescripcionClave(moduloId))
                    .orElse("");
        }

        TrainingModule modulo = moduleRepository.findById(moduloId)
                .orElseThrow(() -> new com.atalayas.backend.exception.ResourceNotFoundException(
                        "Módulo no encontrado: " + moduloId));
        User empleado = userRepository.findById(usuarioId)
                .orElseThrow(() -> new com.atalayas.backend.exception.ResourceNotFoundException(
                        "Usuario no encontrado: " + usuarioId));

        try {
            byte[] pdfBytes = generarPdf(empleado, modulo);
            String filename = "certificado-" + UUID.randomUUID() + ".pdf";
            String url = supabaseStorageService.subirArchivo(
                    pdfBytes, "application/pdf", "documentos", filename);

            Documento doc = Documento.builder()
                    .empresaId(empresaId)
                    .titulo("Certificado de " + modulo.getNombre())
                    .descripcion(buildDescripcionClave(moduloId))
                    .tipo(TipoDocumento.CERTIFICADO)
                    .archivoUrl(url)
                    .archivoNombre(filename)
                    .mimeType("application/pdf")
                    .tamanoBytes((long) pdfBytes.length)
                    .subidoPor(usuarioId)
                    .requiereFirma(false)
                    .activo(true)
                    .build();
            doc = documentoRepository.save(doc);

            asignacionRepository.save(DocumentoAsignacion.builder()
                    .documentoId(doc.getDocumentoId())
                    .usuarioId(usuarioId)
                    .visto(false)
                    .firmado(false)
                    .build());

            notificationService.crearInterna(
                    usuarioId,
                    "CERTIFICADO_GENERADO",
                    "¡Has completado \"" + modulo.getNombre() + "\"! Tu certificado ya está disponible.",
                    "/dashboard/perfil#mis-documentos"
            );

            log.info("Certificado generado - usuario={} módulo={} doc={}", usuarioId, moduloId, doc.getDocumentoId());
            return url;

        } catch (Exception e) {
            throw new IllegalStateException("Error generando PDF del certificado: " + e.getMessage(), e);
        }
    }

    // ── GENERACIÓN DEL PDF ────────────────────────────────────────────────

    /**
     * Genera un PDF de certificado con diseño profesional usando PDFBox 3.x.
     * Orientación horizontal (A4 landscape) para mayor impacto visual.
     */
    private byte[] generarPdf(User empleado, TrainingModule modulo) throws Exception {
        // A4 apaisado: 842 × 595 pts
        PDRectangle pageSize = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(pageSize);
            document.addPage(page);

            float W = pageSize.getWidth();   // 842
            float H = pageSize.getHeight();  // 595

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {

                // ── Fondo blanco ──────────────────────────────────────────
                cs.setNonStrokingColor(Color.WHITE);
                cs.addRect(0, 0, W, H);
                cs.fill();

                // ── Borde exterior azul oscuro ────────────────────────────
                float margin = 20f;
                cs.setStrokingColor(new Color(30, 64, 175));   // blue-800
                cs.setLineWidth(4f);
                cs.addRect(margin, margin, W - 2 * margin, H - 2 * margin);
                cs.stroke();

                // ── Banda superior azul ───────────────────────────────────
                cs.setNonStrokingColor(new Color(30, 64, 175));
                cs.addRect(margin, H - margin - 70, W - 2 * margin, 70);
                cs.fill();

                // ── Texto "CERTIFICADO DE FORMACIÓN" en la banda ──────────
                cs.setNonStrokingColor(Color.WHITE);
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 22f);
                String headerText = "CERTIFICADO DE FORMACIÓN";
                float headerW = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
                        .getStringWidth(headerText) / 1000 * 22f;
                cs.newLineAtOffset((W - headerW) / 2f, H - margin - 45f);
                cs.showText(headerText);
                cs.endText();

                // ── Logo / nombre empresa (EGM Atalayas) ──────────────────
                cs.setNonStrokingColor(new Color(30, 64, 175));
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11f);
                cs.newLineAtOffset(margin + 10, H - margin - 15f);
                cs.showText("EGM ATALAYAS");
                cs.endText();

                // ── "Se certifica que" ────────────────────────────────────
                cs.setNonStrokingColor(new Color(100, 116, 139));  // slate-500
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 14f);
                String sub1 = "Se certifica que";
                float sub1W = new PDType1Font(Standard14Fonts.FontName.HELVETICA)
                        .getStringWidth(sub1) / 1000 * 14f;
                cs.newLineAtOffset((W - sub1W) / 2f, H - margin - 110f);
                cs.showText(sub1);
                cs.endText();

                // ── Nombre del empleado ───────────────────────────────────
                String nombreEmpleado = empleado.getNombre() + " " + empleado.getApellidos();
                cs.setNonStrokingColor(new Color(15, 23, 42));    // slate-900
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE), 30f);
                float nameW = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE)
                        .getStringWidth(nombreEmpleado) / 1000 * 30f;
                cs.newLineAtOffset((W - nameW) / 2f, H - margin - 160f);
                cs.showText(nombreEmpleado);
                cs.endText();

                // ── Línea decorativa bajo el nombre ──────────────────────
                cs.setStrokingColor(new Color(30, 64, 175));
                cs.setLineWidth(1.5f);
                float lineLen = Math.min(nameW + 40, W - 2 * margin - 40);
                cs.moveTo((W - lineLen) / 2f, H - margin - 170f);
                cs.lineTo((W + lineLen) / 2f, H - margin - 170f);
                cs.stroke();

                // ── "ha completado satisfactoriamente el módulo formativo" ─
                cs.setNonStrokingColor(new Color(100, 116, 139));
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 13f);
                String sub2 = "ha completado satisfactoriamente el módulo formativo";
                float sub2W = new PDType1Font(Standard14Fonts.FontName.HELVETICA)
                        .getStringWidth(sub2) / 1000 * 13f;
                cs.newLineAtOffset((W - sub2W) / 2f, H - margin - 200f);
                cs.showText(sub2);
                cs.endText();

                // ── Nombre del módulo ─────────────────────────────────────
                String nombreModulo = modulo.getNombre();
                cs.setNonStrokingColor(new Color(30, 64, 175));
                cs.beginText();

                // Ajustar tamaño si el nombre es muy largo
                float moduloFontSize = 22f;
                PDType1Font moduloFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                float moduloW = moduloFont.getStringWidth(nombreModulo) / 1000 * moduloFontSize;
                while (moduloW > W - 2 * margin - 80 && moduloFontSize > 14f) {
                    moduloFontSize -= 1f;
                    moduloW = moduloFont.getStringWidth(nombreModulo) / 1000 * moduloFontSize;
                }
                cs.setFont(moduloFont, moduloFontSize);
                cs.newLineAtOffset((W - moduloW) / 2f, H - margin - 240f);
                cs.showText(nombreModulo);
                cs.endText();

                // ── Fecha de expedición ───────────────────────────────────
                String fecha = "Expedido el " + LocalDate.now()
                        .format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy",
                                new java.util.Locale("es", "ES")));
                cs.setNonStrokingColor(new Color(100, 116, 139));
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11f);
                float fechaW = new PDType1Font(Standard14Fonts.FontName.HELVETICA)
                        .getStringWidth(fecha) / 1000 * 11f;
                cs.newLineAtOffset((W - fechaW) / 2f, H - margin - 285f);
                cs.showText(fecha);
                cs.endText();

                // ── Banda inferior ────────────────────────────────────────
                cs.setNonStrokingColor(new Color(241, 245, 249));  // slate-100
                cs.addRect(margin, margin, W - 2 * margin, 50f);
                cs.fill();

                // ── Texto "Formación Profesional · EGM Atalayas" ──────────
                cs.setNonStrokingColor(new Color(148, 163, 184));  // slate-400
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9f);
                String footer = "Formación Profesional · EGM Atalayas · www.egmatalayas.com";
                float footerW = new PDType1Font(Standard14Fonts.FontName.HELVETICA)
                        .getStringWidth(footer) / 1000 * 9f;
                cs.newLineAtOffset((W - footerW) / 2f, margin + 18f);
                cs.showText(footer);
                cs.endText();

                // ── ID de verificación ────────────────────────────────────
                cs.setNonStrokingColor(new Color(203, 213, 225));  // slate-300
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 7f);
                String idText = "Ref: " + UUID.randomUUID().toString().toUpperCase().substring(0, 8);
                cs.newLineAtOffset(margin + 10, margin + 8f);
                cs.showText(idText);
                cs.endText();
            }

            document.save(out);
            return out.toByteArray();
        }
    }

    // ── HELPERS ───────────────────────────────────────────────────────────

    /**
     * Clave única que almacenamos en 'descripcion' del Documento para poder
     * detectar si ya generamos el certificado (evitar duplicados).
     */
    private String buildDescripcionClave(UUID moduloId) {
        return "cert:modulo:" + moduloId;
    }
}
