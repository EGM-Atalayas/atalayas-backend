package com.atalayas.backend.documento;

import com.atalayas.backend.documento.dto.AsignacionDetalleResponse;
import com.atalayas.backend.documento.dto.DocumentoResponse;
import com.atalayas.backend.documento.dto.DocumentoUploadRequest;
import com.atalayas.backend.documento.enums.TipoDocumento;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Endpoints de gestión documental.
 *
 * <p>Estructura:
 * <ul>
 *     <li>{@code POST /documentos}                  — admin sube un documento y lo asigna</li>
 *     <li>{@code GET  /documentos}                  — admin lista los docs de su empresa</li>
 *     <li>{@code GET  /documentos/{id}/asignaciones} — admin ve estado por empleado</li>
 *     <li>{@code DELETE /documentos/{id}}           — admin desactiva (soft delete)</li>
 *     <li>{@code GET  /documentos/me}               — empleado lista sus documentos asignados</li>
 *     <li>{@code POST /documentos/me/{id}/visto}    — empleado marca como visto</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/documentos")
@RequiredArgsConstructor
@Tag(name = "Documentos", description = "Gestión documental: subida, asignación, visualización")
public class DocumentoController {

    private final DocumentoService documentoService;
    private final ObjectMapper objectMapper;

    // ── ADMIN ───────────────────────────────────────────────────────────────

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Subir documento y asignarlo a uno o varios empleados")
    public ResponseEntity<DocumentoResponse> subir(
            @RequestParam("file") MultipartFile file,
            @RequestParam("titulo") String titulo,
            @RequestParam(value = "descripcion", required = false) String descripcion,
            @RequestParam("tipo") TipoDocumento tipo,
            @RequestParam(value = "requiereFirma", defaultValue = "false") boolean requiereFirma,
            @RequestParam(value = "asignarATodos", defaultValue = "false") boolean asignarATodos,
            @RequestParam(value = "notificar", defaultValue = "true") boolean notificar,
            @RequestParam(value = "usuariosIds", required = false) List<UUID> usuariosIds,
            @RequestParam(value = "departamentos", required = false) List<String> departamentos
    ) {
        DocumentoUploadRequest req = new DocumentoUploadRequest();
        req.setTitulo(titulo);
        req.setDescripcion(descripcion);
        req.setTipo(tipo);
        req.setRequiereFirma(requiereFirma);
        req.setAsignarATodos(asignarATodos);
        req.setNotificar(notificar);
        req.setUsuariosIds(usuariosIds);
        req.setDepartamentos(departamentos);

        DocumentoResponse resp = documentoService.subirDocumento(file, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Listar documentos de la empresa (con totales de visto/firmado)")
    public ResponseEntity<List<DocumentoResponse>> listarEmpresa() {
        return ResponseEntity.ok(documentoService.listarDocumentosEmpresa());
    }

    @GetMapping("/{id}/asignaciones")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Ver detalle de asignaciones de un documento")
    public ResponseEntity<List<AsignacionDetalleResponse>> listarAsignaciones(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(documentoService.listarAsignaciones(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Desactivar documento (soft delete)")
    public ResponseEntity<Void> desactivar(@PathVariable("id") UUID id) {
        documentoService.desactivarDocumento(id);
        return ResponseEntity.noContent().build();
    }

    // ── EMPLEADO ────────────────────────────────────────────────────────────

    @GetMapping("/me")
    @Operation(summary = "Listar documentos asignados al usuario autenticado")
    public ResponseEntity<List<DocumentoResponse>> misDocumentos() {
        return ResponseEntity.ok(documentoService.listarMisDocumentos());
    }

    @PostMapping("/me/{id}/visto")
    @Operation(summary = "Marcar un documento asignado como visto")
    public ResponseEntity<Void> marcarVisto(@PathVariable("id") UUID id) {
        documentoService.marcarVisto(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/certificado/{moduloId}")
    @Operation(summary = "Obtener URL del certificado auto-generado para un módulo")
    public ResponseEntity<Map<String, String>> getCertificadoModulo(
            @PathVariable("moduloId") UUID moduloId) {
        return documentoService.obtenerUrlCertificadoModulo(moduloId)
                .map(url -> ResponseEntity.ok(Map.of("url", url)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
