package com.atalayas.backend.progress.controller;

import com.atalayas.backend.progress.dto.CompleteContentRequest;
import com.atalayas.backend.progress.dto.ProgressResponse;
import com.atalayas.backend.progress.service.ProgressService;
import com.atalayas.backend.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


/**
 * Endpoints para trazabilidad de progreso formativo
 *
 * Acceso por rol:
 *   POST registrar     - cualquier usuario autenticado (empleado solo el suyo)
 *   GET  mi-progreso   - cualquier usuario autenticado
 *   GET  por contenido - cualquier usuario autenticado
 *   GET  por usuario   - admin empresa (solo su empresa) y superadmin
 *   GET  por empresa   - admin empresa (solo la suya) y superadmin
 */
@RestController
@RequestMapping("/api/v1/progreso")
@RequiredArgsConstructor
@Tag(name = "Progreso", description = "Trazabilidad de progreso formativo por empleado")
@SecurityRequirement(name = "bearerAuth")
public class ProgressController {

    private final ProgressService progressService;


    /**
     * POST /api/v1/progreso
     * Registra o actualiza el progreso de un empleado sobre un contenido
     * El tiempo se acumula - el front envía solo el tiempo de la sesión actual
     * Completado es irreversible: una vez marcado no se puede desmarcar
     * Empleado solo puede registrar su propio progreso - 403 si intenta el de otro
     */
    @PostMapping
    @Operation(summary = "Registrar o actualizar progreso sobre un contenido")
    public ResponseEntity<ProgressResponse> registrarProgreso(
            @Valid @RequestBody CompleteContentRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(progressService.registrarProgreso(request, user));
    }


    /**
     * GET /api/v1/progreso/me
     * Devuelve todo el progreso del usuario autenticado ordenado por última actividad
     */
    @GetMapping("/me")
    @Operation(summary = "Obtener mi progreso completo")
    public ResponseEntity<List<ProgressResponse>> miProgreso(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(progressService.miProgreso(user));
    }


    /**
     * GET /api/v1/progreso/contenido/{contenidoId}
     * Devuelve el estado del usuario autenticado sobre un contenido concreto
     * Si no hay registro previo devuelve estado PENDIENTE virtual (sin persistir)
     */
    @GetMapping("/contenido/{contenidoId}")
    @Operation(summary = "Obtener mi progreso sobre un contenido concreto")
    public ResponseEntity<ProgressResponse> progresoPorContenido(
            @PathVariable UUID contenidoId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(progressService.progresoPorContenido(contenidoId, user));
    }


    /**
     * GET /api/v1/progreso/usuario/{usuarioId}
     * Devuelve el progreso de un empleado concreto
     * Admin empresa solo puede consultar empleados de su empresa - 403 si es ajeno
     */
    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Obtener progreso de un empleado - admin empresa solo ve los de su empresa")
    public ResponseEntity<List<ProgressResponse>> progresoPorUsuario(
            @PathVariable UUID usuarioId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(progressService.progresoPorUsuario(usuarioId, user));
    }


    /**
     * GET /api/v1/progreso/empresa/{empresaId}
     * Devuelve todo el progreso de los empleados de una empresa
     * Útil para el dashboard de admin empresa
     * Admin empresa solo puede consultar su propia empresa - 403 si es ajena
     */
    @GetMapping("/empresa/{empresaId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Obtener progreso de toda una empresa para dashboard de admin")
    public ResponseEntity<List<ProgressResponse>> progresoPorEmpresa(
            @PathVariable UUID empresaId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(progressService.progresoPorEmpresa(empresaId, user));
    }
}