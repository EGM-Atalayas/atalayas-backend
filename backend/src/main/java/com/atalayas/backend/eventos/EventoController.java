package com.atalayas.backend.eventos;

import com.atalayas.backend.eventos.dto.EventoRequest;
import com.atalayas.backend.eventos.dto.EventoResponse;
import com.atalayas.backend.usuario.entity.User;
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
 * Endpoints para eventos del área empresarial.
 *
 * GET  - cualquier usuario autenticado
 * POST / PUT / PATCH - solo ROLE_ADMIN (superadmin)
 */
@RestController
@RequestMapping("/api/v1/eventos")
@RequiredArgsConstructor
@Tag(name = "Eventos", description = "Eventos del área empresarial EGM Atalayas")
@SecurityRequirement(name = "bearerAuth")
public class EventoController {

    private final EventoService eventoService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Crear evento - solo superadmin")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Evento creado"),
        @ApiResponse(responseCode = "403", description = "Rol insuficiente",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<EventoResponse> crear(
            @Valid @RequestBody EventoRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoService.crear(request, user));
    }

    @GetMapping
    @Operation(summary = "Listar eventos activos - cualquier usuario autenticado")
    @ApiResponse(responseCode = "200", description = "Lista de eventos")
    public ResponseEntity<List<EventoResponse>> listar() {
        return ResponseEntity.ok(eventoService.listar());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Actualizar evento - solo superadmin")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Evento actualizado"),
        @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<EventoResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody EventoRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(eventoService.actualizar(id, request, user));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Cancelar evento (soft delete) - solo superadmin")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Evento cancelado"),
        @ApiResponse(responseCode = "400", description = "El evento ya estaba cancelado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<EventoResponse> cancelar(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(eventoService.cancelar(id, user));
    }
}
