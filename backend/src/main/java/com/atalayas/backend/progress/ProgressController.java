package com.atalayas.backend.progress;

import com.atalayas.backend.common.dto.PaginatedResponse;
import com.atalayas.backend.moduloprogreso.ModuloProgresoService;
import com.atalayas.backend.moduloprogreso.dto.EmpleadoProgresoResponse;
import com.atalayas.backend.progress.dto.CompleteContentRequest;
import com.atalayas.backend.progress.dto.ProgressResponse;
import com.atalayas.backend.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


/**
 * Endpoints para trazabilidad de progreso formativo
 *
 * Acceso por rol:
 *   POST registrar     - cualquier usuario autenticado (empleado solo el suyo)
 *   GET  mi-progreso   - cualquier usuario autenticado
 *   GET  por contenido - cualquier usuario autenticado
 *   GET  por usuario   - admin empresa (solo su empresa) y superadmin
 *   GET  por empresa   - admin empresa (solo la suya) y superadmin
 */
@RestController
@RequestMapping("/api/v1/progreso")
@RequiredArgsConstructor
@Tag(name = "Progreso", description = "Trazabilidad de progreso formativo por empleado")
@SecurityRequirement(name = "bearerAuth")
public class ProgressController {

    private final ProgressService progressService;
    private final ModuloProgresoService moduloProgresoService;


    /**
     * POST /api/v1/progreso
     * Registra o actualiza el progreso de un empleado sobre un contenido
     * El tiempo se acumula - el front envía solo el tiempo de la sesión actual
     * Completado es irreversible: una vez marcado no se puede desmarcar
     * Empleado solo puede registrar su propio progreso - 403 si intenta el de otro
     */
    @PostMapping
    @Operation(summary = "Registrar o actualizar progreso sobre un contenido",
               description = "El tiempo se acumula — el front envía solo el tiempo de la sesión actual. `completado` es **irreversible**. Empleado solo puede registrar su propio progreso.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Progreso registrado/actualizado"),
        @ApiResponse(responseCode = "403", description = "Intento de registrar progreso de otro usuario",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Contenido no encontrado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<ProgressResponse> registrarProgreso(
            @Valid @RequestBody CompleteContentRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(progressService.registrarProgreso(request, user));
    }


    /**
     * GET /api/v1/progreso/me
     * Devuelve todo el progreso del usuario autenticado ordenado por última actividad
     */
    @GetMapping("/me")
    @Operation(summary = "Obtener mi progreso completo",
               description = "Devuelve todo el progreso del usuario autenticado ordenado por última actividad.")
    @ApiResponse(responseCode = "200", description = "Lista de progresos del usuario")
    public ResponseEntity<List<ProgressResponse>> miProgreso(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(progressService.miProgreso(user));
    }


    /**
     * GET /api/v1/progreso/contenido/{contenidoId}
     * Devuelve el estado del usuario autenticado sobre un contenido concreto
     * Si no hay registro previo devuelve estado PENDIENTE virtual (sin persistir)
     */
    @GetMapping("/contenido/{contenidoId}")
    @Operation(summary = "Obtener mi progreso sobre un contenido concreto",
               description = "Si no hay registro previo devuelve estado `PENDIENTE` virtual (sin persistir).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estado del progreso sobre el contenido"),
        @ApiResponse(responseCode = "404", description = "Contenido no encontrado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<ProgressResponse> progresoPorContenido(
            @PathVariable UUID contenidoId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(progressService.progresoPorContenido(contenidoId, user));
    }


    /**
     * GET /api/v1/progreso/usuario/{usuarioId}
     * Devuelve el progreso de un empleado concreto
     * Admin empresa solo puede consultar empleados de su empresa - 403 si es ajeno
     */
    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Obtener progreso de un empleado - admin empresa solo ve los de su empresa")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Progreso del empleado"),
        @ApiResponse(responseCode = "403", description = "El usuario pertenece a otra empresa",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<List<ProgressResponse>> progresoPorUsuario(
            @PathVariable UUID usuarioId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(progressService.progresoPorUsuario(usuarioId, user));
    }


    /**
     * GET /api/v1/progreso/empresa/{empresaId}
     * Devuelve todos los empleados de la empresa con su progreso en cada módulo.
     * Útil para el dashboard de admin empresa.
     * Admin empresa solo puede consultar su propia empresa - 403 si es ajena.
     */
    @GetMapping("/empresa/{empresaId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Obtener progreso de toda una empresa para dashboard de admin")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Progreso de todos los empleados de la empresa"),
        @ApiResponse(responseCode = "403", description = "La empresa no es la tuya",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Empresa no encontrada",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<List<EmpleadoProgresoResponse>> progresoPorEmpresa(
            @PathVariable UUID empresaId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(moduloProgresoService.progresoEmpresa(empresaId, user));
    }

    @GetMapping("/empresa/{empresaId}/paginado")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Obtener progreso paginado de una empresa para dashboard de admin")
    @ApiResponse(responseCode = "200", description = "Página de progreso de empleados")
    public ResponseEntity<PaginatedResponse<ProgressResponse>> progresoPorEmpresaPaged(
            @PathVariable UUID empresaId,
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ResponseEntity.ok(progressService.progresoPorEmpresaPaged(empresaId, user, page, size));
    }
}