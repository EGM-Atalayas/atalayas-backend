package com.atalayas.backend.documento;

import com.atalayas.backend.ai.service.SupabaseStorageService;
import com.atalayas.backend.common.util.SecurityUtils;
import com.atalayas.backend.communication.service.NotificationService;
import com.atalayas.backend.documento.dto.AsignacionDetalleResponse;
import com.atalayas.backend.documento.dto.DocumentoResponse;
import com.atalayas.backend.documento.dto.DocumentoUploadRequest;
import com.atalayas.backend.documento.dto.FirmarDocumentoRequest;
import com.atalayas.backend.documento.dto.FirmarDocumentoResponse;
import com.atalayas.backend.documento.entity.Documento;
import com.atalayas.backend.documento.entity.DocumentoAsignacion;
import com.atalayas.backend.documento.mapper.DocumentoMapper;
import com.atalayas.backend.documento.repository.DocumentoAsignacionRepository;
import com.atalayas.backend.documento.repository.DocumentoRepository;
import com.atalayas.backend.exception.ResourceNotFoundException;
import com.atalayas.backend.user.entity.User;
import com.atalayas.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de gestión documental.
 *
 * Reglas de visibilidad:
 * - ADMIN_EMPRESA: opera solo sobre documentos de su propia empresa.
 * - ADMIN (superadmin): operación cross-empresa (sin filtros).
 * - EMPLEADO: solo ve los documentos que le han sido asignados.
 *
 * Subida: el archivo se sube a Supabase Storage al bucket público
 * "modulos" bajo la carpeta "documentos/". Las URLs son obscure-by-UUID;
 * más adelante puede migrarse a un bucket privado con signed URLs si
 * se requiere mayor confidencialidad (p. ej. nóminas).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentoService {

    private final DocumentoRepository documentoRepository;
    private final DocumentoAsignacionRepository asignacionRepository;
    private final UserRepository userRepository;
    private final SupabaseStorageService supabaseStorageService;
    private final NotificationService notificationService;
    private final DocumentoMapper mapper;

    // ── ADMIN ───────────────────────────────────────────────────────────────

    @Transactional
    public DocumentoResponse subirDocumento(MultipartFile file, DocumentoUploadRequest request) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo no puede estar vacío");
        }
        if (file.getSize() > 25L * 1024 * 1024) {
            throw new IllegalArgumentException("El archivo supera el tamaño máximo de 25 MB");
        }

        User admin = SecurityUtils.getCurrentUser();
        UUID empresaId = admin.getEmpresaId();
        if (empresaId == null) {
            throw new AccessDeniedException("Usuario sin empresa asignada");
        }

        // 1. Subir a Supabase Storage
        String extension = extraerExtension(file.getOriginalFilename());
        String filename  = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
        String url;
        try {
            url = supabaseStorageService.subirArchivo(
                    file.getBytes(),
                    file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                    "documentos",
                    filename
            );
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo leer el archivo: " + e.getMessage(), e);
        }

        // 2. Persistir Documento
        Documento doc = Documento.builder()
                .empresaId(empresaId)
                .titulo(request.getTitulo())
                .descripcion(request.getDescripcion())
                .tipo(request.getTipo())
                .archivoUrl(url)
                .archivoNombre(file.getOriginalFilename() != null ? file.getOriginalFilename() : filename)
                .mimeType(file.getContentType())
                .tamanoBytes(file.getSize())
                .subidoPor(admin.getUsuarioId())
                .requiereFirma(request.isRequiereFirma())
                .activo(true)
                .build();
        doc = documentoRepository.save(doc);

        // 3. Resolver destinatarios
        Set<UUID> destinatarios = resolverDestinatarios(empresaId, request);

        // 4. Crear asignaciones
        for (UUID userId : destinatarios) {
            asignacionRepository.save(
                    DocumentoAsignacion.builder()
                            .documentoId(doc.getDocumentoId())
                            .usuarioId(userId)
                            .visto(false)
                            .firmado(false)
                            .build()
            );
        }

        // 5. Notificaciones
        if (request.isNotificar()) {
            for (UUID userId : destinatarios) {
                try {
                    notificationService.crearInterna(
                            userId,
                            "DOCUMENTO_NUEVO",
                            "Tienes un nuevo documento disponible: " + doc.getTitulo(),
                            "/dashboard/perfil#mis-documentos"
                    );
                } catch (Exception e) {
                    log.warn("No se pudo notificar a {} sobre documento {}: {}",
                            userId, doc.getDocumentoId(), e.getMessage());
                }
            }
        }

        log.info("Documento subido - id={} empresa={} destinatarios={}",
                doc.getDocumentoId(), empresaId, destinatarios.size());

        DocumentoResponse resp = mapper.toResponse(doc);
        resp.setTotalAsignados(destinatarios.size());
        resp.setTotalVistos(0);
        resp.setTotalFirmados(0);
        return resp;
    }

    @Transactional(readOnly = true)
    public List<DocumentoResponse> listarDocumentosEmpresa() {
        UUID empresaId = SecurityUtils.getEmpresaId();
        List<Documento> docs = documentoRepository.findByEmpresaIdAndActivoTrueOrderByFechaSubidaDesc(empresaId);

        return docs.stream().map(d -> {
            DocumentoResponse r = mapper.toResponse(d);
            r.setTotalAsignados((int) asignacionRepository.findByDocumentoId(d.getDocumentoId()).size());
            r.setTotalVistos((int) asignacionRepository.countByDocumentoIdAndVistoTrue(d.getDocumentoId()));
            r.setTotalFirmados((int) asignacionRepository.countByDocumentoIdAndFirmadoTrue(d.getDocumentoId()));
            return r;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AsignacionDetalleResponse> listarAsignaciones(UUID documentoId) {
        Documento doc = obtenerDocumentoEmpresa(documentoId);
        List<DocumentoAsignacion> asigs = asignacionRepository.findByDocumentoId(doc.getDocumentoId());

        // Cargar usuarios en batch
        Map<UUID, User> userMap = userRepository.findAllById(
                asigs.stream().map(DocumentoAsignacion::getUsuarioId).toList()
        ).stream().collect(Collectors.toMap(User::getUsuarioId, u -> u));

        return asigs.stream()
                .map(a -> mapper.toAsignacionDetalle(a, userMap.get(a.getUsuarioId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public void desactivarDocumento(UUID documentoId) {
        Documento doc = obtenerDocumentoEmpresa(documentoId);
        doc.setActivo(false);
        documentoRepository.save(doc);
    }

    // ── EMPLEADO ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DocumentoResponse> listarMisDocumentos() {
        UUID userId = SecurityUtils.getCurrentUser().getUsuarioId();
        List<DocumentoAsignacion> asigs = asignacionRepository.findByUsuarioIdOrderByFechaAsignacionDesc(userId);
        if (asigs.isEmpty()) return List.of();

        Map<UUID, Documento> docMap = documentoRepository.findAllById(
                asigs.stream().map(DocumentoAsignacion::getDocumentoId).toList()
        ).stream().collect(Collectors.toMap(Documento::getDocumentoId, d -> d));

        return asigs.stream()
                .map(a -> {
                    Documento d = docMap.get(a.getDocumentoId());
                    if (d == null || !d.isActivo()) return null;
                    return mapper.toResponseConAsignacion(d, a);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Transactional
    public void marcarVisto(UUID documentoId) {
        UUID userId = SecurityUtils.getCurrentUser().getUsuarioId();
        DocumentoAsignacion a = asignacionRepository.findByDocumentoIdAndUsuarioId(documentoId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no asignado al usuario"));
        if (!a.isVisto()) {
            a.setVisto(true);
            a.setFechaVisto(java.time.OffsetDateTime.now());
            asignacionRepository.save(a);
        }
    }

    /**
     * Estampa la firma del empleado sobre el PDF original y guarda el resultado en Supabase.
     *
     * Flujo:
     *  1. Verificar que el documento está asignado al usuario y requiere firma.
     *  2. Descargar el PDF original desde su URL pública.
     *  3. Decodificar la imagen PNG de la firma (base64 del canvas frontend).
     *  4. Con PDFBox, añadir la imagen en la esquina inferior derecha de la última página.
     *  5. Subir el PDF firmado a Supabase bajo "documentos/firmados/".
     *  6. Actualizar DocumentoAsignacion: firmado=true, fechaFirma, firmaUrl.
     */
    @Transactional
    public FirmarDocumentoResponse firmarDocumento(UUID documentoId, FirmarDocumentoRequest request) {
        UUID userId = SecurityUtils.getCurrentUser().getUsuarioId();

        // 1. Cargar asignación
        DocumentoAsignacion asignacion = asignacionRepository
                .findByDocumentoIdAndUsuarioId(documentoId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no asignado al usuario"));

        if (asignacion.isFirmado()) {
            throw new IllegalStateException("El documento ya fue firmado");
        }

        Documento doc = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));

        if (!doc.isRequiereFirma()) {
            throw new IllegalArgumentException("Este documento no requiere firma");
        }

        // 2. Descargar PDF original
        byte[] pdfOriginal;
        try (InputStream in = URI.create(doc.getArchivoUrl()).toURL().openStream()) {
            pdfOriginal = in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo descargar el PDF original: " + e.getMessage(), e);
        }

        // 3. Decodificar firma base64 (quitar prefijo data URI si lo tiene)
        String b64 = request.getFirmaBase64();
        if (b64.contains(",")) b64 = b64.substring(b64.indexOf(',') + 1);
        byte[] firmaBytes = Base64.getDecoder().decode(b64);

        // 4. Estampar firma con PDFBox
        byte[] pdfFirmado;
        try (PDDocument document = PDDocument.load(pdfOriginal);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Última página
            int lastPageIndex = document.getNumberOfPages() - 1;
            PDPage lastPage = document.getPage(lastPageIndex);
            float pageW = lastPage.getMediaBox().getWidth();
            float pageH = lastPage.getMediaBox().getHeight();

            // Imagen de la firma
            PDImageXObject firmaImg = PDImageXObject.createFromByteArray(document, firmaBytes, "firma");

            // Posición: esquina inferior derecha, con margen
            float firmaW = 160f;
            float firmaH = firmaImg.getHeight() * (firmaW / firmaImg.getWidth());
            float x = pageW - firmaW - 40f;
            float y = 30f;

            try (PDPageContentStream cs = new PDPageContentStream(
                    document, lastPage, PDPageContentStream.AppendMode.APPEND, true, true)) {

                // Línea de firma
                cs.setStrokingColor(new java.awt.Color(180, 180, 180));
                cs.setLineWidth(0.5f);
                cs.moveTo(x - 5, y + firmaH + 5);
                cs.lineTo(x + firmaW + 5, y + firmaH + 5);
                cs.stroke();

                // Imagen firma
                cs.drawImage(firmaImg, x, y, firmaW, firmaH);

                // Texto "Firmado digitalmente"
                cs.beginText();
                cs.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(
                        org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA), 7f);
                cs.setNonStrokingColor(new java.awt.Color(100, 116, 139));
                cs.newLineAtOffset(x, y - 8f);
                cs.showText("Firmado digitalmente · " +
                        java.time.LocalDate.now().format(
                                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                cs.endText();
            }

            document.save(out);
            pdfFirmado = out.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException("Error al procesar el PDF: " + e.getMessage(), e);
        }

        // 5. Subir PDF firmado a Supabase
        String filename = "firmado-" + UUID.randomUUID() + ".pdf";
        String firmaUrl = supabaseStorageService.subirArchivo(
                pdfFirmado, "application/pdf", "documentos/firmados", filename);

        // 6. Actualizar asignación
        asignacion.setFirmado(true);
        asignacion.setFechaFirma(OffsetDateTime.now());
        asignacion.setFirmaUrl(firmaUrl);
        asignacionRepository.save(asignacion);

        log.info("Documento firmado - doc={} usuario={} firmaUrl={}", documentoId, userId, firmaUrl);
        return new FirmarDocumentoResponse(firmaUrl);
    }

    /**
     * Devuelve la URL del certificado auto-generado para un módulo concreto.
     * El frontend lo llama al pulsar "Descargar certificado": si existe la versión
     * guardada en Supabase la abre directamente; si no, genera localmente con jsPDF.
     */
    public Optional<String> obtenerUrlCertificadoModulo(UUID moduloId) {
        UUID userId = SecurityUtils.getCurrentUser().getUsuarioId();
        String clave = "cert:modulo:" + moduloId;
        return asignacionRepository.findCertificadoUrl(userId, clave);
    }

    // ── INTERNOS ────────────────────────────────────────────────────────────

    private Documento obtenerDocumentoEmpresa(UUID documentoId) {
        Documento d = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));
        if (!SecurityUtils.isSuperAdmin()) {
            UUID empresaId = SecurityUtils.getEmpresaId();
            if (!d.getEmpresaId().equals(empresaId)) {
                // 404 enmascarado para no revelar existencia
                throw new ResourceNotFoundException("Documento no encontrado");
            }
        }
        return d;
    }

    private Set<UUID> resolverDestinatarios(UUID empresaId, DocumentoUploadRequest req) {
        Set<UUID> result = new HashSet<>();

        if (req.isAsignarATodos()) {
            userRepository.findAllByEmpresaId(empresaId).stream()
                    .filter(User::isActivo)
                    .map(User::getUsuarioId)
                    .forEach(result::add);
        }

        if (req.getDepartamentos() != null && !req.getDepartamentos().isEmpty()) {
            Set<String> dptos = new HashSet<>(req.getDepartamentos());
            userRepository.findAllByEmpresaId(empresaId).stream()
                    .filter(User::isActivo)
                    .filter(u -> u.getDepartamento() != null && dptos.contains(u.getDepartamento()))
                    .map(User::getUsuarioId)
                    .forEach(result::add);
        }

        if (req.getUsuariosIds() != null) {
            // Validar que pertenecen a la empresa
            userRepository.findAllById(req.getUsuariosIds()).stream()
                    .filter(u -> empresaId.equals(u.getEmpresaId()))
                    .map(User::getUsuarioId)
                    .forEach(result::add);
        }

        if (result.isEmpty()) {
            throw new IllegalArgumentException(
                    "Debes asignar el documento al menos a un empleado, departamento o a toda la empresa");
        }
        return result;
    }

    private String extraerExtension(String filename) {
        if (filename == null) return "";
        int i = filename.lastIndexOf('.');
        if (i < 0 || i == filename.length() - 1) return "";
        return filename.substring(i + 1).toLowerCase();
    }
}
