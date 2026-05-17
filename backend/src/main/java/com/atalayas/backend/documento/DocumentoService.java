package com.atalayas.backend.documento;

import com.atalayas.backend.ai.service.SupabaseStorageService;
import com.atalayas.backend.common.util.SecurityUtils;
import com.atalayas.backend.communication.service.NotificationService;
import com.atalayas.backend.documento.dto.AsignacionDetalleResponse;
import com.atalayas.backend.documento.dto.DocumentoAsignarRequest;
import com.atalayas.backend.documento.dto.DocumentoDesasignarRequest;
import com.atalayas.backend.documento.dto.DocumentoResponse;
import com.atalayas.backend.documento.dto.DocumentoUpdateRequest;
import com.atalayas.backend.documento.dto.DocumentoUploadRequest;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
    public DocumentoResponse actualizarDocumento(UUID documentoId, DocumentoUpdateRequest request) {
        Documento doc = obtenerDocumentoEmpresa(documentoId);
        doc.setTitulo(request.getTitulo().trim());
        doc.setDescripcion(request.getDescripcion() != null && !request.getDescripcion().isBlank()
                ? request.getDescripcion().trim() : null);
        doc.setTipo(request.getTipo());
        doc.setRequiereFirma(request.isRequiereFirma());
        doc = documentoRepository.save(doc);

        DocumentoResponse resp = mapper.toResponse(doc);
        resp.setTotalAsignados((int) asignacionRepository.findByDocumentoId(doc.getDocumentoId()).size());
        resp.setTotalVistos((int) asignacionRepository.countByDocumentoIdAndVistoTrue(doc.getDocumentoId()));
        resp.setTotalFirmados((int) asignacionRepository.countByDocumentoIdAndFirmadoTrue(doc.getDocumentoId()));
        return resp;
    }

    @Transactional
    public void añadirAsignaciones(UUID documentoId, DocumentoAsignarRequest request) {
        User admin = SecurityUtils.getCurrentUser();
        UUID empresaId = admin.getEmpresaId();
        if (empresaId == null) throw new AccessDeniedException("Usuario sin empresa asignada");

        Documento doc = obtenerDocumentoEmpresa(documentoId);

        // Resolver destinatarios usando la misma lógica que la subida
        // Reutilizamos DocumentoUploadRequest como adaptador
        DocumentoUploadRequest uploadReq = new DocumentoUploadRequest();
        uploadReq.setAsignarATodos(request.isAsignarATodos());
        uploadReq.setUsuariosIds(request.getUsuariosIds());
        uploadReq.setDepartamentos(request.getDepartamentos());
        Set<UUID> destinatarios = resolverDestinatarios(empresaId, uploadReq);

        // Ya asignados — evitar duplicados (la tabla tiene unique constraint)
        Set<UUID> yaAsignados = asignacionRepository.findByDocumentoId(documentoId)
                .stream().map(DocumentoAsignacion::getUsuarioId).collect(Collectors.toSet());

        int nuevos = 0;
        for (UUID userId : destinatarios) {
            if (yaAsignados.contains(userId)) continue;
            asignacionRepository.save(
                    DocumentoAsignacion.builder()
                            .documentoId(doc.getDocumentoId())
                            .usuarioId(userId)
                            .visto(false)
                            .firmado(false)
                            .build()
            );
            nuevos++;

            if (request.isNotificar()) {
                try {
                    notificationService.crearInterna(
                            userId,
                            "DOCUMENTO_NUEVO",
                            "Tienes un nuevo documento disponible: " + doc.getTitulo(),
                            "/dashboard/perfil#mis-documentos"
                    );
                } catch (Exception e) {
                    log.warn("No se pudo notificar a {} sobre documento {}: {}", userId, documentoId, e.getMessage());
                }
            }
        }
        log.info("Asignaciones añadidas - docId={} nuevas={}", documentoId, nuevos);
    }

    @Transactional
    public void eliminarAsignaciones(UUID documentoId, DocumentoDesasignarRequest request) {
        Documento doc = obtenerDocumentoEmpresa(documentoId);

        // Verificar que las asignaciones pertenecen a este documento
        List<DocumentoAsignacion> aEliminar = asignacionRepository.findAllById(request.getAsignacionIds())
                .stream()
                .filter(a -> a.getDocumentoId().equals(doc.getDocumentoId()))
                .toList();

        if (aEliminar.isEmpty()) {
            throw new ResourceNotFoundException("No se encontraron asignaciones válidas para eliminar");
        }

        asignacionRepository.deleteAll(aEliminar);
        log.info("Asignaciones eliminadas - docId={} cantidad={}", documentoId, aEliminar.size());
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
     * Devuelve la URL del certificado auto-generado para un módulo concreto.
     * El frontend lo llama al pulsar "Descargar certificado": si existe la versión
     * guardada en Supabase la abre directamente; si no, genera localmente con jsPDF.
     */
    public Optional<String> obtenerUrlCertificadoModulo(UUID moduloId) {
        UUID userId = SecurityUtils.getCurrentUser().getUsuarioId();
        String clave = "cert:modulo:" + moduloId;
        return asignacionRepository.findCertificadoUrl(userId, clave);
    }

    /**
     * Estampa la firma manuscrita (PNG en Base64) sobre la última página del PDF
     * y sube el resultado a Supabase como nuevo archivo.
     *
     * @param documentoId  UUID del documento (asignado al usuario actual)
     * @param firmaBase64  Imagen PNG de la firma, codificada en Base64 (sin prefijo data:...)
     * @return URL pública del PDF firmado en Supabase
     */
    @Transactional
    public String firmarDocumento(UUID documentoId, String firmaBase64) {
        UUID userId = SecurityUtils.getCurrentUser().getUsuarioId();

        // 1. Obtener asignación
        DocumentoAsignacion asig = asignacionRepository
                .findByDocumentoIdAndUsuarioId(documentoId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no asignado al usuario"));

        Documento doc = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));

        if (!doc.isActivo()) throw new ResourceNotFoundException("Documento no disponible");
        if (!doc.isRequiereFirma()) throw new IllegalStateException("Este documento no requiere firma");
        if (asig.isFirmado()) throw new IllegalStateException("El documento ya ha sido firmado");

        // 2. Descargar PDF original desde Supabase (URL pública)
        byte[] pdfBytes = descargarBytes(doc.getArchivoUrl());

        // 3. Decodificar firma PNG
        String base64Clean = firmaBase64.replaceFirst("^data:image/[^;]+;base64,", "");
        byte[] firmaBytes = java.util.Base64.getDecoder().decode(base64Clean);

        // 4. Estampar firma sobre la última página con PDFBox
        byte[] pdfFirmado;
        try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {
            int lastPageIdx = pdf.getNumberOfPages() - 1;
            var lastPage = pdf.getPage(lastPageIdx);
            var mediaBox = lastPage.getMediaBox();

            PDImageXObject firmaImg = PDImageXObject.createFromByteArray(pdf, firmaBytes, "firma");

            // Área de firma: esquina inferior derecha, máximo 200x60 pts manteniendo proporción
            float maxW = 200f, maxH = 60f;
            float imgW = firmaImg.getWidth(), imgH = firmaImg.getHeight();
            float scale = Math.min(maxW / imgW, maxH / imgH);
            float drawW = imgW * scale, drawH = imgH * scale;
            float margin = 36f;
            float x = mediaBox.getWidth() - drawW - margin;
            float y = margin;

            try (PDPageContentStream cs = new PDPageContentStream(
                    pdf, lastPage, PDPageContentStream.AppendMode.APPEND, true, true)) {
                cs.drawImage(firmaImg, x, y, drawW, drawH);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pdf.save(out);
            pdfFirmado = out.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo procesar el PDF para la firma: " + e.getMessage(), e);
        }

        // 5. Subir PDF firmado a Supabase
        String filename = "firmado-" + UUID.randomUUID() + ".pdf";
        String firmaUrl = supabaseStorageService.subirArchivo(pdfFirmado, "application/pdf", "documentos", filename);

        // 6. Persistir estado de firma en la asignación
        asig.setFirmado(true);
        asig.setFechaFirma(OffsetDateTime.now());
        asig.setFirmaUrl(firmaUrl);
        // Marcar también como visto si no lo estaba
        if (!asig.isVisto()) {
            asig.setVisto(true);
            asig.setFechaVisto(OffsetDateTime.now());
        }
        asignacionRepository.save(asig);

        log.info("Documento firmado - docId={} userId={} firmaUrl={}", documentoId, userId, firmaUrl);
        return firmaUrl;
    }

    // ── INTERNOS ────────────────────────────────────────────────────────────

    /** Descarga bytes de una URL pública (Supabase Storage u otra CDN). */
    private byte[] descargarBytes(String url) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() != 200) {
                throw new IllegalStateException("No se pudo descargar el archivo: HTTP " + resp.statusCode());
            }
            return resp.body();
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Error descargando el archivo original: " + e.getMessage(), e);
        }
    }

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
