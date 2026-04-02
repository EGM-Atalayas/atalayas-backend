package com.atalayas.backend.communication.controller;

import com.atalayas.backend.communication.dto.NotificacionRequest;
import com.atalayas.backend.communication.dto.NotificacionResponse;
import com.atalayas.backend.communication.service.NotificacionService;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notificaciones")
@RequiredArgsConstructor
@Tag(name = "Notificaciones", description = "Notificaciones individuales por usuario")
@SecurityRequirement(name = "bearerAuth")
public class NotificacionController {

    private final NotificacionService notificacionService;


    /**
     * POST /api/v1/notificaciones
     * Creación manual de notificación — solo ROLE_ADMIN y ROLE_ADMIN_EMPRESA
     * - Sin sesión   - 401
     * - ROLE_EMPLEADO - 403
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Crear notificación manual - ROLE_ADMIN y ROLE_ADMIN_EMPRESA")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Notificación creada"),
        @ApiResponse(responseCode = "403", description = "Rol insuficiente",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Usuario destinatario no encontrado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<NotificacionResponse> crear(
            @Valid @RequestBody NotificacionRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(notificacionService.crear(request, user));
    }


    /**
     * GET /api/v1/notificaciones/me
     * Todas las notificaciones del usuario autenticado (leídas + no leídas)
     * - Sin sesión - 401
     */
    @GetMapping("/me")
    @Operation(summary = "Listar todas mis notificaciones",
               description = "Devuelve todas las notificaciones del usuario autenticado, leídas y no leídas.")
    @ApiResponse(responseCode = "200", description = "Lista de notificaciones")
    public ResponseEntity<List<NotificacionResponse>> listarMias(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificacionService.listarMias(user));
    }


    /**
     * GET /api/v1/notificaciones/me/no-leidas
     * Solo las notificaciones no leídas - para la campana del frontend
     * - Sin sesión - 401
     */
    @GetMapping("/me/no-leidas")
    @Operation(summary = "Listar mis notificaciones no leídas",
               description = "Usado para la campana de notificaciones del frontend.")
    @ApiResponse(responseCode = "200", description = "Lista de notificaciones no leídas")
    public ResponseEntity<List<NotificacionResponse>> listarMisNoLeidas(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificacionService.listarMisNoLeidas(user));
    }


    /**
     * GET /api/v1/notificaciones/me/contador
     * Número de notificaciones no leídas - ligero para pooling periódico
     * - Sin sesión - 401
     */
    @GetMapping("/me/contador")
    @Operation(summary = "Contador de notificaciones no leídas",
               description = "Endpoint ligero para polling periódico. Devuelve `{ \"noLeidas\": N }`.")
    @ApiResponse(responseCode = "200", description = "Número de notificaciones no leídas")
    public ResponseEntity<Map<String, Long>> contarNoLeidas(
            @AuthenticationPrincipal User user) {
        long total = notificacionService.contarNoLeidas(user);
        return ResponseEntity.ok(Map.of("noLeidas", total));
    }


    /**
     * PATCH /api/v1/notificaciones/{id}/leer
     * Marca una notificación individual como leída
     * Solo el propio destinatario puede ejecutarlo
     * - 404 si no existe
     * - 400 si ya estaba leída
     * - 400 si no eres el destinatario (seguridad cross-user)
     */
    @PatchMapping("/{id}/leer")
    @Operation(summary = "Marcar notificación como leída",
               description = "Solo el destinatario puede marcar sus propias notificaciones. Devuelve `400` si ya estaba leída.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notificación marcada como leída"),
        @ApiResponse(responseCode = "400", description = "La notificación ya estaba leída o no eres el destinatario",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Notificación no encontrada",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<NotificacionResponse> marcarComoLeida(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificacionService.marcarComoLeida(id, user));
    }


    /**
     * PATCH /api/v1/notificaciones/me/leer-todas
     * Marca todas las notificaciones no leídas del usuario como leídas
     * Devuelve cuántas se han actualizado
     * - Sin sesión - 401
     */
    @PatchMapping("/me/leer-todas")
    @Operation(summary = "Marcar todas mis notificaciones como leídas",
               description = "Devuelve `{ \"actualizadas\": N }` con el número de notificaciones actualizadas.")
    @ApiResponse(responseCode = "200", description = "Todas las notificaciones marcadas como leídas")
    public ResponseEntity<Map<String, Integer>> marcarTodasComoLeidas(
            @AuthenticationPrincipal User user) {
        int actualizadas = notificacionService.marcarTodasComoLeidas(user);
        return ResponseEntity.ok(Map.of("actualizadas", actualizadas));
    }
}