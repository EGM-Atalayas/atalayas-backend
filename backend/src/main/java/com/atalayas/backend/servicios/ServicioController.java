package com.atalayas.backend.servicios;

import com.atalayas.backend.servicios.dto.ServicioRequest;
import com.atalayas.backend.servicios.dto.ServicioResponse;
import com.atalayas.backend.servicios.enums.CategoriaServicio;
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
 * Endpoints para gestión de servicios del área empresarial EGM Atalayas.
 *
 * Acceso:
 *   GET  - cualquier usuario autenticado
 *   POST / PUT / PATCH - solo superadmin (ROLE_ADMIN)
 */
@RestController
@RequestMapping("/api/v1/servicios")
@RequiredArgsConstructor
@Tag(name = "Servicios", description = "Servicios del área empresarial EGM Atalayas")
@SecurityRequirement(name = "bearerAuth")
public class ServicioController {

    private final ServicioService servicioService;


    /**
     * POST /api/v1/servicios
     * Crea un servicio. Solo superadmin.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Crear servicio - solo superadmin")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Servicio creado"),
        @ApiResponse(responseCode = "403", description = "Rol insuficiente",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<ServicioResponse> crear(
            @Valid @RequestBody ServicioRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(servicioService.crear(request, user));
    }


    /**
     * GET /api/v1/servicios
     * Lista servicios activos. Filtro opcional por categoría.
     * Cualquier usuario autenticado puede consultar.
     */
    @GetMapping
    @Operation(summary = "Listar servicios activos",
               description = "Opcionalmente filtrar por categoría: MOVILIDAD, INSTALACIONES, INICIATIVAS, COMUNES")
    @ApiResponse(responseCode = "200", description = "Lista de servicios")
    public ResponseEntity<List<ServicioResponse>> listar(
            @RequestParam(required = false) CategoriaServicio categoria) {
        return ResponseEntity.ok(servicioService.listar(categoria));
    }


    /**
     * PUT /api/v1/servicios/{id}
     * Actualiza un servicio. Solo superadmin.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Actualizar servicio - solo superadmin")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Servicio actualizado"),
        @ApiResponse(responseCode = "403", description = "Rol insuficiente",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Servicio no encontrado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<ServicioResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ServicioRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(servicioService.actualizar(id, request, user));
    }


    /**
     * PATCH /api/v1/servicios/{id}/desactivar
     * Soft delete del servicio. Solo superadmin.
     */
    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Desactivar servicio (soft delete) - solo superadmin")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Servicio desactivado"),
        @ApiResponse(responseCode = "400", description = "El servicio ya estaba desactivado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "403", description = "Rol insuficiente",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Servicio no encontrado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<ServicioResponse> desactivar(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(servicioService.desactivar(id, user));
    }
}
