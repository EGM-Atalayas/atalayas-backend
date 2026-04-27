package com.atalayas.backend.incidencia;

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
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Tag(name = "Incidencias", description = "Gestión de incidencias de la plataforma (superadmin)")
@SecurityRequirement(name = "bearerAuth")
public class IncidenciaController {

    private final IncidenciaService incidenciaService;

    @PostMapping
    @Operation(summary = "Crear nueva incidencia")
    public ResponseEntity<IncidenciaResponse> crear(@Valid @RequestBody IncidenciaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidenciaService.crear(request));
    }

    @GetMapping
    @Operation(summary = "Listar todas las incidencias ordenadas por fecha DESC")
    public ResponseEntity<List<IncidenciaResponse>> listar() {
        return ResponseEntity.ok(incidenciaService.listarTodas());
    }

    @PatchMapping("/{id}/cerrar")
    @Operation(summary = "Cerrar una incidencia")
    public ResponseEntity<IncidenciaResponse> cerrar(@PathVariable Long id) {
        return ResponseEntity.ok(incidenciaService.cerrar(id));
    }
}

