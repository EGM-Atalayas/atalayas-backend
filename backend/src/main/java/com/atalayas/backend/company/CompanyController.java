package com.atalayas.backend.company;

import com.atalayas.backend.common.dto.PaginatedResponse;
import com.atalayas.backend.common.enums.EstadoSolicitud;
import com.atalayas.backend.company.dto.AccionSolicitudRequest;
import com.atalayas.backend.company.dto.CambioEstadoRequest;
import com.atalayas.backend.company.dto.CompanyResponse;
import com.atalayas.backend.company.dto.ReenviarEmailRequest;
import com.atalayas.backend.company.dto.SolicitudAltaEmpresaRequest;
import com.atalayas.backend.company.dto.SolicitudAltaEmpresaResponse;
import com.atalayas.backend.company.dto.SolicitudPendienteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/empresas")
@RequiredArgsConstructor
@Tag(name = "Empresas", description = "Gestión del ciclo de vida de empresas en la plataforma")
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping("/solicitud")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Solicitar alta de empresa",
               description = "Endpoint **público** — no requiere autenticación. Crea la empresa en estado `PENDIENTE` y su usuario administrador con `activo = false`. La cuenta se activa cuando el SUPER_ADMIN apruebe la solicitud.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Solicitud creada correctamente",
                     content = @Content(schema = @Schema(implementation = SolicitudAltaEmpresaResponse.class))),
        @ApiResponse(responseCode = "400", description = "CIF o email ya registrado / datos inválidos",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<SolicitudAltaEmpresaResponse> crear(@Valid @RequestBody SolicitudAltaEmpresaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyService.crearEmpresa(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Listar todas las empresas (SUPER_ADMIN)")
    public ResponseEntity<List<CompanyResponse>> getAll() {
        return ResponseEntity.ok(companyService.getAll());
    }

    @GetMapping("/paginado")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Listar empresas paginadas con filtros opcionales (SUPER_ADMIN)")
    public ResponseEntity<PaginatedResponse<CompanyResponse>> getAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) EstadoSolicitud estado) {
        return ResponseEntity.ok(companyService.getAllPaged(page, size, search, estado));
    }

    @GetMapping("/pendientes")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Listar empresas pendientes de resolución (SUPER_ADMIN)")
    public ResponseEntity<List<CompanyResponse>> getPendientes() {
        return ResponseEntity.ok(companyService.getPendientes());
    }

    @GetMapping("/aprobadas")
    @Operation(summary = "Listar empresas aprobadas — público, usado en el selector de registro")
    public ResponseEntity<List<CompanyResponse>> getAprobadas() {
        return ResponseEntity.ok(companyService.getAprobadas());
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cambiar estado de empresa (SUPER_ADMIN)",
               description = """
                   Gestiona el ciclo de vida de una empresa activa. Transiciones válidas:

                   | Desde | Hacia | Efecto |
                   |---|---|---|
                   | `PENDIENTE` | `APROBADA` | Activa usuarios, email bienvenida, notificación interna |
                   | `APROBADA` | `PAUSADA` | Desactiva empresa y usuarios temporalmente |
                   | `PAUSADA` | `APROBADA` | Reactiva empresa y usuarios |

                   > El rechazo de solicitudes (con eliminación física) solo es posible desde `PATCH /{id}/solicitud`.
                   """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estado actualizado correctamente",
                     content = @Content(schema = @Schema(implementation = CompanyResponse.class))),
        @ApiResponse(responseCode = "400", description = "Transición no permitida o empresa en mismo estado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "401", description = "Sin autenticación",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "403", description = "Rol insuficiente — requiere ROLE_ADMIN",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Empresa no encontrada",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<CompanyResponse> cambiarEstado(
            @PathVariable UUID id,
            @Valid @RequestBody CambioEstadoRequest request) {
        return ResponseEntity.ok(companyService.cambiarEstado(id, request));
    }

    /**
     * PATCH /api/v1/empresas/{id}/activacion
     * Activa o desactiva una empresa APROBADA (toggle de activo).
     * No cambia estadoSolicitud. Los empleados de una empresa inactiva no pueden iniciar sesión.
     */
    @PatchMapping("/{id}/activacion")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Activar / desactivar empresa aprobada (SUPER_ADMIN)",
               description = "Alterna el campo `activo` de la empresa y de todos sus usuarios. Una empresa inactiva impide el acceso a sus empleados.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estado de activación actualizado",
                     content = @Content(schema = @Schema(implementation = CompanyResponse.class))),
        @ApiResponse(responseCode = "400", description = "La empresa no está aprobada"),
        @ApiResponse(responseCode = "404", description = "Empresa no encontrada")
    })
    public ResponseEntity<CompanyResponse> toggleActivacion(@PathVariable UUID id) {
        return ResponseEntity.ok(companyService.toggleActivacion(id));
    }

    /**
     * GET /api/v1/empresas/solicitudes
     * Lista empresas en estado PENDIENTE con datos del admin provisional.
     * Usada en la pantalla de solicitudes del superadmin.
     */
    @GetMapping("/solicitudes")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Listar solicitudes pendientes de aprobación (SUPER_ADMIN)",
               description = "Devuelve las empresas en estado `PENDIENTE` con el nombre y email del admin provisional.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de solicitudes pendientes"),
        @ApiResponse(responseCode = "401", description = "Sin autenticación",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "403", description = "Rol insuficiente",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<List<SolicitudPendienteResponse>> getSolicitudesPendientes() {
        return ResponseEntity.ok(companyService.getSolicitudesPendientes());
    }

    /**
     * PATCH /api/v1/empresas/{id}/solicitud
     * Aprueba o rechaza una solicitud de alta.
     * Body: { "accion": "aprobar" } o { "accion": "rechazar" }
     * Respuesta: 200 OK sin cuerpo.
     */
    @PatchMapping("/{id}/solicitud")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Aprobar o rechazar una solicitud de empresa (SUPER_ADMIN)",
               description = "Envía `accion: \"aprobar\"` o `accion: \"rechazar\"`. Devuelve 200 sin cuerpo.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Acción aplicada correctamente"),
        @ApiResponse(responseCode = "400", description = "Acción inválida o transición no permitida",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Empresa no encontrada",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<Void> resolverSolicitud(
            @PathVariable UUID id,
            @Valid @RequestBody AccionSolicitudRequest request) {
        companyService.resolverSolicitud(id, request);
        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/v1/empresas/{id}/reenviar-email
     * Reenvía el email de aprobación al admin de la empresa cuando emailEnviado=false.
     * Devuelve 502 si el envío SMTP falla.
     */
    @PostMapping("/{id}/reenviar-email")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Reenviar email de aprobación (SUPER_ADMIN)",
               description = "Reenvía el email de aprobación cuando el envío automático falló (`emailEnviado = false`). " +
                             "Actualiza `email_enviado = true` si tiene éxito. Devuelve 502 si SMTP falla.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Email enviado correctamente"),
        @ApiResponse(responseCode = "400", description = "Tipo inválido o empresa no APROBADA"),
        @ApiResponse(responseCode = "404", description = "Empresa no encontrada"),
        @ApiResponse(responseCode = "502", description = "Fallo SMTP")
    })
    public ResponseEntity<Void> reenviarEmail(
            @PathVariable UUID id,
            @Valid @RequestBody ReenviarEmailRequest request) {
        companyService.reenviarEmail(id, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Subir logo de empresa",
               description = "Sube el logo de la empresa a Supabase Storage y actualiza `logoUrl`. " +
                             "`ROLE_ADMIN_EMPRESA` solo puede actualizar el logo de su propia empresa.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logo actualizado",
                     content = @Content(schema = @Schema(implementation = CompanyResponse.class))),
        @ApiResponse(responseCode = "403", description = "Intentando modificar el logo de otra empresa",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Empresa no encontrada",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<CompanyResponse> subirLogo(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(companyService.subirLogo(id, file));
    }
}

