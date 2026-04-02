package com.atalayas.backend.module.controller;

import com.atalayas.backend.module.dto.ModuleRequest;
import com.atalayas.backend.module.dto.ModuleResponse;
import com.atalayas.backend.module.service.ModuleService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


/**
 * Endpoints para la gestión de módulos formativos
 *
 * Acceso por rol:
 *   GET    - cualquier usuario autenticado (empleado ve solo los suyos + globales)
 *   POST   - admin empresa y superadmin
 *   PUT    - admin empresa (solo los suyos) y superadmin
 *   PATCH  - admin empresa (solo los suyos) y superadmin
 */
@RestController
@RequestMapping("/api/v1/modulos")
@RequiredArgsConstructor
@Tag(name = "Módulos", description = "Gestión de módulos formativos por empresa")
@SecurityRequirement(name = "bearerAuth")
public class ModuleController {

    private final ModuleService moduleService;


    /**
     * POST /api/v1/modulos
     * Crea un nuevo módulo, Admin empresa solo puede crear en su empresa
     * Superadmin puede crear módulos globales (empresaId = null)
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Crear módulo - admin empresa crea en su empresa, superadmin puede crear globales")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Módulo creado"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos o módulo de empresa ajena",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "401", description = "Sin autenticación",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "403", description = "Rol insuficiente",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<ModuleResponse> crear(
            @Valid @RequestBody ModuleRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(moduleService.crear(request, user));
    }


    /**
     * GET /api/v1/modulos
     * Devuelve los módulos visibles para el usuario autenticado
     *   Empleado: activos de su empresa + globales activos
     *   Admin empresa: todos los de su empresa + globales activos
     *   Superadmin: todos los activos de la plataforma
     */
    @GetMapping
    @Operation(summary = "Listar módulos visibles para el usuario autenticado",
               description = "Empleado: activos de su empresa + globales. Admin empresa: todos los suyos + globales. Superadmin: todos.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de módulos"),
        @ApiResponse(responseCode = "401", description = "Sin autenticación",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<List<ModuleResponse>> listar(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(moduleService.listar(user));
    }


    /**
     * GET /api/v1/modulos/{id}
     * Devuelve un módulo por ID
     * Devuelve 403 si el módulo no pertenece a la empresa del usuario
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtener módulo por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Módulo encontrado"),
        @ApiResponse(responseCode = "403", description = "El módulo no pertenece a tu empresa",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Módulo no encontrado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<ModuleResponse> obtenerPorId(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(moduleService.obtenerPorId(id, user));
    }


    /**
     * PUT /api/v1/modulos/{id}
     * Actualiza todos los campos de un módulo
     * Admin empresa solo puede editar los módulos de su empresa - 403 si es ajeno
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Actualizar módulo — admin empresa solo puede editar los propios")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Módulo actualizado"),
        @ApiResponse(responseCode = "403", description = "Módulo de otra empresa",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Módulo no encontrado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<ModuleResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ModuleRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(moduleService.actualizar(id, request, user));
    }


    /**
     * PATCH /api/v1/modulos/{id}/desactivar
     * Soft delete: marca el módulo como inactivo
     * Admin empresa solo puede desactivar los suyos - 403 si es ajeno o global
     */
    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Desactivar módulo (soft delete) - admin empresa solo puede desactivar los propios")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Módulo desactivado"),
        @ApiResponse(responseCode = "400", description = "El módulo ya estaba desactivado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "403", description = "Módulo de otra empresa o global",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Módulo no encontrado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<ModuleResponse> desactivar(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(moduleService.desactivar(id, user));
    }
}