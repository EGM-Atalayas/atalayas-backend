package com.atalayas.backend.moduloprogreso;

import com.atalayas.backend.moduloprogreso.dto.GuardarProgresoRequest;
import com.atalayas.backend.moduloprogreso.dto.ModuloProgresoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints de progreso del empleado por módulo formativo.
 *
 * <ul>
 *   <li>{@code POST /modulos/{moduloId}/progreso} — guardar / actualizar progreso</li>
 *   <li>{@code GET  /modulos/me/progreso}          — todos los progresos del usuario</li>
 *   <li>{@code GET  /modulos/{moduloId}/progreso/me} — progreso del usuario en un módulo</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/modulos")
@RequiredArgsConstructor
@Tag(name = "Módulo Progreso", description = "Progreso del empleado por módulo formativo")
public class ModuloProgresoController {

    private final ModuloProgresoService service;

    @PostMapping("/{moduloId}/progreso")
    @Operation(summary = "Guardar / actualizar el progreso del usuario en un módulo")
    public ResponseEntity<ModuloProgresoResponse> guardar(
            @PathVariable UUID moduloId,
            @RequestBody @Valid GuardarProgresoRequest body) {
        return ResponseEntity.ok(service.guardarProgreso(moduloId, body));
    }

    @GetMapping("/me/progreso")
    @Operation(summary = "Listar todos los progresos del usuario autenticado")
    public ResponseEntity<List<ModuloProgresoResponse>> misProgresos() {
        return ResponseEntity.ok(service.miProgreso());
    }

    @GetMapping("/{moduloId}/progreso/me")
    @Operation(summary = "Obtener el progreso del usuario en un módulo concreto")
    public ResponseEntity<ModuloProgresoResponse> miProgresoModulo(@PathVariable UUID moduloId) {
        return ResponseEntity.ok(service.miProgresoModulo(moduloId));
    }

    @DeleteMapping("/{moduloId}/progreso")
    @Operation(summary = "Reiniciar el progreso del usuario autenticado en un módulo")
    public ResponseEntity<Void> reiniciarProgreso(@PathVariable UUID moduloId) {
        service.reiniciarProgreso(moduloId);
        return ResponseEntity.noContent().build();
    }
}
