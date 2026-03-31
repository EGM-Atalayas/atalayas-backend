package com.atalayas.backend.community.controller;

import com.atalayas.backend.community.dto.CommunityEventRequest;
import com.atalayas.backend.community.dto.CommunityEventResponse;
import com.atalayas.backend.community.service.CommunityEventService;
import com.atalayas.backend.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "Listar eventos visibles para el usuario autenticado")
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
    public ResponseEntity<CommunityEventResponse> desactivar(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(communityEventService.desactivar(id, user));
    }
}