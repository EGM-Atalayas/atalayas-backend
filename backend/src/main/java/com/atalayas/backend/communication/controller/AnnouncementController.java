package com.atalayas.backend.communication.controller;

import com.atalayas.backend.communication.dto.AnnouncementRequest;
import com.atalayas.backend.communication.dto.AnnouncementResponse;
import com.atalayas.backend.communication.service.AnnouncementService;
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
    @Operation(summary = "Listar anuncios visibles para el usuario autenticado")
    public ResponseEntity<List<AnnouncementResponse>> listar(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(announcementService.listar(user));
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
    public ResponseEntity<AnnouncementResponse> desactivar(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(announcementService.desactivar(id, user));
    }
}

