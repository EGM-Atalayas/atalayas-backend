package com.atalayas.backend.rewards;

import com.atalayas.backend.rewards.dto.BenefitRequest;
import com.atalayas.backend.rewards.dto.BenefitResponse;
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
 * Endpoints para gestión de beneficios y ventajas del área empresarial
 *
 * Acceso por rol:
 *   GET  - cualquier usuario autenticado
 *   POST / PUT / PATCH - admin empresa (solo los suyos) y superadmin
 */
@RestController
@RequestMapping("/api/v1/beneficios")
@RequiredArgsConstructor
@Tag(name = "Beneficios", description = "Gestión de beneficios y ventajas del área empresarial")
@SecurityRequirement(name = "bearerAuth")
public class BenefitController {

    private final BenefitService benefitService;


    /**
     * POST /api/v1/beneficios
     * Crea un beneficio
     * Superadmin puede crear globales (empresaId = null)
     * Admin empresa siempre crea en su empresa
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Crear beneficio - superadmin puede crear globales, admin empresa solo los suyos")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Beneficio creado"),
        @ApiResponse(responseCode = "403", description = "Rol insuficiente",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<BenefitResponse> crear(
            @Valid @RequestBody BenefitRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(benefitService.crear(request, user));
    }


    /**
     * GET /api/v1/beneficios
     * Lista los beneficios visibles para el usuario autenticado
     * Empleado: activos de su empresa + globales activos
     * Superadmin: todos los activos de la plataforma
     */
    @GetMapping
    @Operation(summary = "Listar beneficios visibles para el usuario autenticado",
               description = "Empleado: activos de su empresa + globales. Superadmin: todos los activos.")
    @ApiResponse(responseCode = "200", description = "Lista de beneficios")
    public ResponseEntity<List<BenefitResponse>> listar(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(benefitService.listar(user));
    }


    /**
     * PUT /api/v1/beneficios/{id}
     * Actualiza un beneficio
     * Admin empresa solo puede editar los suyos → 403 si es ajeno o global
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Actualizar beneficio - admin empresa solo puede editar los propios")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Beneficio actualizado"),
        @ApiResponse(responseCode = "403", description = "Beneficio de otra empresa o global",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Beneficio no encontrado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<BenefitResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody BenefitRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(benefitService.actualizar(id, request, user));
    }


    /**
     * PATCH /api/v1/beneficios/{id}/desactivar
     * Soft delete del beneficio
     * Admin empresa solo puede desactivar los suyos → 403 si es ajeno o global
     */
    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Desactivar beneficio (soft delete) - admin empresa solo puede desactivar los propios")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Beneficio desactivado"),
        @ApiResponse(responseCode = "400", description = "El beneficio ya estaba desactivado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "403", description = "Beneficio de otra empresa o global",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Beneficio no encontrado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<BenefitResponse> desactivar(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(benefitService.desactivar(id, user));
    }
}