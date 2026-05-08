package com.atalayas.backend.communication.controller;

import com.atalayas.backend.communication.dto.AnnouncementRequest;
import com.atalayas.backend.communication.dto.AnnouncementResponse;
import com.atalayas.backend.communication.service.AnnouncementService;
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
@RequestMapping("/api/v1/anuncios")
@RequiredArgsConstructor
@Tag(name = "Anuncios", description = "Gestión de anuncios por empresa y globales")
@SecurityRequirement(name = "bearerAuth")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    /**
     * POST /api/v1/anuncios
     * ROLE_ADMIN puede crear anuncios globales (esGlobal=true).
     * ROLE_ADMIN_EMPRESA solo crea para su empresa (esGlobal se ignora → false).
     * - Sin sesión   → 401
     * - Rol empleado → 403
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Crear anuncio — ROLE_ADMIN puede crear globales, ROLE_ADMIN_EMPRESA solo para su empresa")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Anuncio creado"),
        @ApiResponse(responseCode = "403", description = "Rol insuficiente",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<AnnouncementResponse> crear(
            @Valid @RequestBody AnnouncementRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(announcementService.crear(request, user));
    }

    /**
     * GET /api/v1/anuncios
     * Cualquier usuario autenticado ve los anuncios de su empresa + los globales.
     * ROLE_ADMIN ve todos los activos de la plataforma.
     * - Sin sesión → 401
     */
    @GetMapping
    @Operation(summary = "Listar anuncios visibles para el usuario autenticado",
               description = "Cualquier usuario ve los de su empresa + globales. ROLE_ADMIN ve todos los activos de la plataforma.")
    @ApiResponse(responseCode = "200", description = "Lista de anuncios")
    public ResponseEntity<List<AnnouncementResponse>> listar(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(announcementService.listar(user));
    }

    /**
     * PUT /api/v1/anuncios/{id}
     * Edita un anuncio existente.
     * ROLE_ADMIN puede editar cualquier anuncio.
     * ROLE_ADMIN_EMPRESA solo puede editar los suyos → 403 si es ajeno/global.
     * - Anuncio no existe    → 404
     * - Anuncio de otra emp  → 403
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Editar anuncio — ROLE_ADMIN_EMPRESA solo puede editar los propios")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Anuncio actualizado"),
        @ApiResponse(responseCode = "403", description = "Anuncio de otra empresa",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Anuncio no encontrado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<AnnouncementResponse> editar(
            @PathVariable UUID id,
            @Valid @RequestBody AnnouncementRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(announcementService.editar(id, request, user));
    }

    /**
     * PATCH /api/v1/anuncios/{id}/vistas
     * Incrementa el contador de vistas. No requiere rol específico — cualquier usuario autenticado.
     */
    @PatchMapping("/{id}/vistas")
    @Operation(summary = "Registrar vista de un anuncio")
    @ApiResponse(responseCode = "204", description = "Vista registrada")
    public ResponseEntity<Void> registrarVista(@PathVariable UUID id) {
        announcementService.registrarVista(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/v1/anuncios/{id}/desactivar
     * ROLE_ADMIN desactiva cualquier anuncio.
     * ROLE_ADMIN_EMPRESA solo puede desactivar los suyos → 403 si es ajeno/global.
     * - Anuncio no existe    → 404
     * - Ya desactivado       → 400
     * - Anuncio de otra emp  → 403
     */
    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Desactivar anuncio — ROLE_ADMIN_EMPRESA solo puede desactivar los propios")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Anuncio desactivado"),
        @ApiResponse(responseCode = "400", description = "El anuncio ya estaba desactivado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "403", description = "Anuncio de otra empresa",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Anuncio no encontrado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<AnnouncementResponse> desactivar(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(announcementService.desactivar(id, user));
    }

    /**
     * DELETE /api/v1/anuncios/{id}
     * Alias REST semántico de PATCH /{id}/desactivar — realiza soft-delete (activo = false).
     * El frontend usa este verbo; la lógica de negocio es idéntica.
     * - Anuncio no existe    → 404
     * - Ya desactivado       → 400
     * - Anuncio de otra emp  → 403
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Desactivar anuncio (soft-delete) — alias REST de PATCH /{id}/desactivar")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Anuncio desactivado"),
        @ApiResponse(responseCode = "400", description = "El anuncio ya estaba desactivado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "403", description = "Anuncio de otra empresa",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Anuncio no encontrado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<AnnouncementResponse> desactivarDelete(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(announcementService.desactivar(id, user));
    }
}
