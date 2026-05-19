package com.atalayas.backend.incidencia;

import com.atalayas.backend.incidencia.dto.CambiarEstadoRequest;
import com.atalayas.backend.incidencia.dto.IncidenciaRequest;
import com.atalayas.backend.incidencia.dto.IncidenciaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/incidencias")
@RequiredArgsConstructor
@Tag(name = "Incidencias", description = "Gestión de incidencias")
@SecurityRequirement(name = "bearerAuth")
public class IncidenciaController {

    private final IncidenciaService incidenciaService;

    /**
     * Cualquier usuario autenticado puede reportar una incidencia.
     * El servicio asigna automáticamente el empresaId del token si no se envía.
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_EMPLEADO')")
    @Operation(summary = "Crear nueva incidencia")
    public ResponseEntity<IncidenciaResponse> crear(@Valid @RequestBody IncidenciaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidenciaService.crear(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Listar incidencias — superadmin: todas; admin empresa: filtradas por empresa")
    public ResponseEntity<List<IncidenciaResponse>> listar() {
        return ResponseEntity.ok(incidenciaService.listarTodas());
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Cambiar el estado de una incidencia (ABIERTA | EN_CURSO | CERRADA)")
    public ResponseEntity<IncidenciaResponse> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoRequest request) {
        return ResponseEntity.ok(incidenciaService.cambiarEstado(id, request.getEstado()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Eliminar una incidencia definitivamente")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        incidenciaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
