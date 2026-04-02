package com.atalayas.backend.communication.controller;

import com.atalayas.backend.communication.dto.ComunicadoRequest;
import com.atalayas.backend.communication.dto.ComunicadoResponse;
import com.atalayas.backend.communication.service.ComunicadoService;
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

@RestController
@RequestMapping("/api/v1/comunicados")
@RequiredArgsConstructor
@Tag(name = "Comunicados", description = "Comunicados oficiales de EGM Atalayas para toda la plataforma")
@SecurityRequirement(name = "bearerAuth")
public class ComunicadoController {

    private final ComunicadoService comunicadoService;


    /**
     * POST /api/v1/comunicados
     * Solo ROLE_ADMIN puede crear comunicados oficiales de EGM
     * - Sin sesión - 401
     * - Otro rol   - 403
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Crear comunicado oficial - solo ROLE_ADMIN")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Comunicado creado"),
        @ApiResponse(responseCode = "403", description = "Rol insuficiente — requiere ROLE_ADMIN",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<ComunicadoResponse> crear(
            @Valid @RequestBody ComunicadoRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(comunicadoService.crear(request, user));
    }


    /**
     * GET /api/v1/comunicados
     * Todos los usuarios autenticados ven los comunicados activos y vigentes
     * ROLE_ADMIN ve el histórico completo incluyendo expirados y desactivados
     * - Sin sesión - 401
     */
    @GetMapping
    @Operation(summary = "Listar comunicados - todos los autenticados ven los vigentes, ROLE_ADMIN ve el histórico",
               description = "Usuarios autenticados ven comunicados activos y vigentes. ROLE_ADMIN ve el histórico completo incluyendo expirados y desactivados.")
    @ApiResponse(responseCode = "200", description = "Lista de comunicados")
    public ResponseEntity<List<ComunicadoResponse>> listar(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(comunicadoService.listar(user));
    }


    /**
     * PATCH /api/v1/comunicados/{id}/desactivar
     * Solo ROLE_ADMIN puede desactivar comunicados
     * - No existe      - 404
     * - Ya desactivado - 400
     * - Otro rol       - 403
     */
    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Desactivar comunicado - solo ROLE_ADMIN")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Comunicado desactivado"),
        @ApiResponse(responseCode = "400", description = "El comunicado ya estaba desactivado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "403", description = "Rol insuficiente",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Comunicado no encontrado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<ComunicadoResponse> desactivar(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(comunicadoService.desactivar(id, user));
    }
}