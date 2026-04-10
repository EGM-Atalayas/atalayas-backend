package com.atalayas.backend.community.controller;

import com.atalayas.backend.community.dto.CommunityEventRequest;
import com.atalayas.backend.community.dto.CommunityEventResponse;
import com.atalayas.backend.community.service.CommunityEventService;
import com.atalayas.backend.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
 * Endpoints para gestión de eventos de comunidad
 *
 * Acceso por rol:
 *   GET  - cualquier usuario autenticado (empleado ve su empresa + globales)
 *   POST / PUT / PATCH - admin empresa (solo los suyos) y superadmin
 */
@RestController
@RequestMapping("/api/v1/eventos")
@RequiredArgsConstructor
@Tag(name = "Comunidad", description = "Gestión de eventos de comunidad por empresa")
@SecurityRequirement(name = "bearerAuth")
public class CommunityEventController {

    private final CommunityEventService communityEventService;


    /**
     * POST /api/v1/eventos
     * Crea un evento de comunidad
     * Admin empresa crea en su empresa
     * Superadmin puede crear globales
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Crear evento - superadmin puede crear globales, admin empresa solo los suyos")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Evento creado"),
        @ApiResponse(responseCode = "403", description = "Rol insuficiente",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<CommunityEventResponse> crear(
            @Valid @RequestBody CommunityEventRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(communityEventService.crear(request, user));
    }


    /**
     * GET /api/v1/eventos
     * Lista los eventos visibles para el usuario autenticado
     * Empleado: activos de su empresa + globales activos
     * Admin empresa: todos los de su empresa + globales activos
     * Superadmin: todos los activos de la plataforma
     */
    @GetMapping
    @Operation(summary = "Listar eventos visibles para el usuario autenticado",
               description = "Empleado: activos de su empresa + globales. Admin empresa: todos los suyos + globales. Superadmin: todos.")
    @ApiResponse(responseCode = "200", description = "Lista de eventos")
    public ResponseEntity<List<CommunityEventResponse>> listar(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(communityEventService.listar(user));
    }


    /**
     * GET /api/v1/eventos/{id}
     * Devuelve un evento por ID
     * Devuelve 403 si el evento no pertenece a la empresa del usuario
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtener evento por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Evento encontrado"),
        @ApiResponse(responseCode = "403", description = "El evento no pertenece a tu empresa",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<CommunityEventResponse> obtenerPorId(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(communityEventService.obtenerPorId(id, user));
    }


    /**
     * PUT /api/v1/eventos/{id}
     * Actualiza un evento, solo superadmin puede cambiar el flag esGlobal
     * Admin empresa solo puede editar los suyos - 403 si es ajeno o global
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Actualizar evento - admin empresa solo puede editar los propios")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Evento actualizado"),
        @ApiResponse(responseCode = "403", description = "Evento de otra empresa o intento de cambiar esGlobal sin ser ADMIN",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<CommunityEventResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody CommunityEventRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(communityEventService.actualizar(id, request, user));
    }


    /**
     * PATCH /api/v1/eventos/{id}/desactivar
     * Soft delete del evento
     * Admin empresa solo puede desactivar los suyos - 403 si es ajeno o global
     */
    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Desactivar evento (soft delete) - admin empresa solo puede desactivar los propios")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Evento desactivado"),
        @ApiResponse(responseCode = "400", description = "El evento ya estaba desactivado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "403", description = "Evento de otra empresa o global",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<CommunityEventResponse> desactivar(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(communityEventService.desactivar(id, user));
    }
}