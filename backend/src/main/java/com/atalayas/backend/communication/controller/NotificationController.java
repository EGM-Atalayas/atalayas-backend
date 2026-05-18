package com.atalayas.backend.communication.controller;

import com.atalayas.backend.communication.dto.NotificationRequest;
import com.atalayas.backend.communication.dto.NotificationResponse;
import com.atalayas.backend.communication.service.NotificationService;
import com.atalayas.backend.usuario.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Endpoints para las notificaciones individuales por usuario
 */
@RestController
@RequestMapping("/api/v1/notificaciones")
@RequiredArgsConstructor
@Tag(name = "Notificaciones", description = "Notificaciones individuales por usuario")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;


    /**
     * POST /api/v1/notificaciones
     * Creación manual de notificación por ROLE_ADMIN o ROLE_ADMIN_EMPRESA.
     * ROLE_EMPLEADO no puede crear notificaciones — 403.
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Crear notificación manual - ROLE_ADMIN y ROLE_ADMIN_EMPRESA")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Notificación creada"),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<NotificationResponse> crear(
            @Valid @RequestBody NotificationRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(notificationService.crear(request, user));
    }


    /**
     * GET /api/v1/notificaciones/me
     * Todas las notificaciones del usuario autenticado, con paginación.
     * Sirve también como endpoint de polling: llamar periódicamente y comparar noLeidas.
     * ?page=0&size=20 (defaults)
     */
    @GetMapping("/me")
    @Operation(summary = "Mis notificaciones paginadas - leídas y no leídas (polling + panel)")
    @ApiResponse(responseCode = "200", description = "Página de notificaciones")
    public ResponseEntity<Page<NotificationResponse>> listarMias(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.listarMiasPaginado(user, page, size));
    }


    /**
     * GET /api/v1/notificaciones/me/no-leidas
     * Solo las no leídas, para el panel lateral de la campana del header.
     */
    @GetMapping("/me/no-leidas")
    @Operation(summary = "Mis notificaciones no leídas - para la campana del header")
    @ApiResponse(responseCode = "200", description = "Lista de notificaciones no leídas")
    public ResponseEntity<List<NotificationResponse>> listarMisNoLeidas(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationService.listarMisNoLeidas(user));
    }


    /**
     * GET /api/v1/notificaciones/me/contador
     * Número de no leídas.
     */
    @GetMapping("/me/contador")
    @Operation(summary = "Contador de no leídas - para el badge de la campana")
    @ApiResponse(responseCode = "200", description = "Número de notificaciones no leídas")
    public ResponseEntity<Map<String, Long>> contarNoLeidas(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("noLeidas", notificationService.contarNoLeidas(user)));
    }


    /**
     * PATCH /api/v1/notificaciones/{id}/leer
     * Marca una notificación individual como leída.
     * 404 si no existe, 400 si ya estaba leída o no eres el destinatario.
     */
    @PatchMapping("/{id}/leer")
    @Operation(summary = "Marcar una notificación como leída")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notificación marcada como leída"),
            @ApiResponse(responseCode = "400", description = "Ya estaba leída o no eres el destinatario",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
            @ApiResponse(responseCode = "404", description = "Notificación no encontrada",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<NotificationResponse> marcarComoLeida(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationService.marcarComoLeida(id, user));
    }


    /**
     * PATCH /api/v1/notificaciones/me/leer-todas
     * Marca todas las no leídas del usuario como leídas de golpe.
     * Se llama cuando el usuario abre el panel de notificaciones.
     * Devuelve { "actualizadas": N }.
     */
    @PatchMapping("/me/leer-todas")
    @Operation(summary = "Marcar todas mis notificaciones como leídas")
    @ApiResponse(responseCode = "200", description = "Todas las notificaciones marcadas como leídas")
    public ResponseEntity<Map<String, Integer>> marcarTodasComoLeidas(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("actualizadas",
                notificationService.marcarTodasComoLeidas(user)));
    }
}