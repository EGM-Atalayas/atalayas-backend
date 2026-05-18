package com.atalayas.backend.communication.controller;

import com.atalayas.backend.communication.dto.OfficialNoticeRequest;
import com.atalayas.backend.communication.dto.OfficialNoticeResponse;
import com.atalayas.backend.communication.service.OfficialNoticeService;
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
 * Endpoints para los comunicados oficiales de EGM Atalayas
 *
 * La URL se mantiene como /api/v1/comunicados para no romper
 * el frontend mientras se actualiza, es un contrato de API público
 */
@RestController
@RequestMapping("/api/v1/comunicados")
@RequiredArgsConstructor
@Tag(name = "Comunicados oficiales",
        description = "Comunicados de EGM Atalayas para toda la plataforma - solo ROLE_ADMIN puede crearlos")
@SecurityRequirement(name = "bearerAuth")
public class OfficialNoticeController {

    private final OfficialNoticeService noticeService;


    /**
     * POST /api/v1/comunicados
     * Crea un comunicado oficial de EGM visible para toda la plataforma.
     * Solo ROLE_ADMIN puede ejecutarlo - 403 para cualquier otro rol.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Crear comunicado oficial - solo ROLE_ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Comunicado creado correctamente"),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente - requiere ROLE_ADMIN",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<OfficialNoticeResponse> crear(
            @Valid @RequestBody OfficialNoticeRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(noticeService.crear(request, user));
    }


    /**
     * GET /api/v1/comunicados
     * ROLE_ADMIN ve el histórico completo incluyendo expirados y desactivados.
     * El resto de roles ven solo los comunicados activos y vigentes.
     */
    @GetMapping
    @Operation(summary = "Listar comunicados - histórico para ROLE_ADMIN, vigentes para el resto")
    @ApiResponse(responseCode = "200", description = "Lista de comunicados")
    public ResponseEntity<List<OfficialNoticeResponse>> listar(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(noticeService.listar(user));
    }


    /**
     * PUT /api/v1/comunicados/{id}
     * Actualiza un comunicado existente (campos + estado borrador/publicado).
     * Solo ROLE_ADMIN puede editar comunicados.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Editar comunicado - solo ROLE_ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comunicado actualizado"),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente"),
            @ApiResponse(responseCode = "404", description = "Comunicado no encontrado")
    })
    public ResponseEntity<OfficialNoticeResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody OfficialNoticeRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(noticeService.actualizar(id, request, user));
    }


    /**
     * PATCH /api/v1/comunicados/{id}/desactivar
     * Soft-delete del comunicado, lo oculta sin borrarlo del histórico.
     * Solo ROLE_ADMIN puede desactivar.
     * 404 si no existe, 400 si ya estaba desactivado.
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
    public ResponseEntity<OfficialNoticeResponse> desactivar(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(noticeService.desactivar(id, user));
    }
}