package com.atalayas.backend.content;

import com.atalayas.backend.content.dto.*;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


/**
 * Endpoints para gestión de contenidos formativos y sus preguntas de evaluación
 *
 * Acceso por rol:
 *   GET    - cualquier usuario autenticado (empleado ve solo activos de su empresa)
 *   POST / PUT / PATCH - admin empresa (solo los suyos) y superadmin
 *   DELETE preguntas   - admin empresa (solo los suyos) y superadmin
 */
@RestController
@RequestMapping("/api/v1/contenidos")
@RequiredArgsConstructor
@Tag(name = "Contenidos", description = "Gestión de contenidos formativos por módulo")
@SecurityRequirement(name = "bearerAuth")
public class ContentController {

    private final ContentService contentService;


    /**
     * POST /api/v1/contenidos
     * Crea un nuevo contenido dentro de un módulo
     * Admin empresa solo puede crear en su empresa
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Crear contenido - admin empresa crea en su empresa, superadmin puede crear globales")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Contenido creado"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos o módulo de empresa ajena",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "403", description = "Rol insuficiente",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Módulo no encontrado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<ContentResponse> crear(
            @Valid @RequestBody ContentRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(contentService.crear(request, user));
    }


    /**
     * GET /api/v1/contenidos/modulo/{moduloId}
     * Lista los contenidos de un módulo
     * Empleado: solo activos. Admin y admin empresa: todos para gestionar
     */
    @GetMapping("/modulo/{moduloId}")
    @Operation(summary = "Listar contenidos de un módulo",
               description = "Empleado solo ve contenidos activos. Admin y admin empresa ven todos para gestionar.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de contenidos"),
        @ApiResponse(responseCode = "403", description = "Módulo de otra empresa",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Módulo no encontrado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<List<ContentResponse>> listarPorModulo(
            @PathVariable UUID moduloId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(contentService.listarPorModulo(moduloId, user));
    }


    /**
     * GET /api/v1/contenidos/{id}
     * Devuelve un contenido por ID con sus preguntas si es EVALUACION
     * Devuelve 403 si el contenido no pertenece a la empresa del usuario
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtener contenido por ID - incluye preguntas si es EVALUACION")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Contenido encontrado"),
        @ApiResponse(responseCode = "403", description = "Contenido de otra empresa",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Contenido no encontrado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<ContentResponse> obtenerPorId(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(contentService.obtenerPorId(id, user));
    }


    /**
     * PUT /api/v1/contenidos/{id}
     * Actualiza un contenido, incrementa versión automáticamente si cambia el cuerpo
     * Admin empresa solo puede editar contenidos de su empresa → 403 si es ajeno
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Actualizar contenido - versión se incrementa si cambia el cuerpo")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Contenido actualizado"),
        @ApiResponse(responseCode = "403", description = "Contenido de otra empresa",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Contenido no encontrado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<ContentResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ContentRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(contentService.actualizar(id, request, user));
    }


    /**
     * PATCH /api/v1/contenidos/{id}/desactivar
     * Soft delete del contenido, la trazabilidad histórica se conserva
     * Admin empresa solo puede desactivar los suyos - 403 si es ajeno o global
     */
    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Desactivar contenido (soft delete) - trazabilidad histórica se conserva")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Contenido desactivado"),
        @ApiResponse(responseCode = "400", description = "El contenido ya estaba desactivado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "403", description = "Contenido de otra empresa",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Contenido no encontrado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<ContentResponse> desactivar(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(contentService.desactivar(id, user));
    }


    // ── PREGUNTAS DE EVALUACIÓN ──────────────────────────────────────────────
    /**
     * POST /api/v1/contenidos/preguntas
     * Añade una pregunta a un contenido de tipo EVALUACION
     */
    @PostMapping("/preguntas")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Añadir pregunta a un contenido de evaluación")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Pregunta añadida"),
        @ApiResponse(responseCode = "400", description = "El contenido no es de tipo EVALUACION",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Contenido no encontrado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<QuestionResponse> crearPregunta(
            @Valid @RequestBody QuestionRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(contentService.crearPregunta(request, user));
    }


    /**
     * DELETE /api/v1/contenidos/preguntas/{preguntaId}
     * Elimina una pregunta de evaluación
     * Admin empresa solo puede eliminar preguntas de sus propios contenidos
     */
    @DeleteMapping("/preguntas/{preguntaId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Eliminar pregunta de evaluación")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Pregunta eliminada"),
        @ApiResponse(responseCode = "403", description = "Pregunta de otra empresa",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Pregunta no encontrada",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<Void> eliminarPregunta(
            @PathVariable UUID preguntaId,
            @AuthenticationPrincipal User user) {
        contentService.eliminarPregunta(preguntaId, user);
        return ResponseEntity.noContent().build();
    }
}