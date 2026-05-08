package com.atalayas.backend.sugerencia;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sugerencias")
@RequiredArgsConstructor
@Tag(name = "Sugerencias", description = "Buzón de sugerencias de empleados")
public class SugerenciaController {

    private final SugerenciaService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Enviar una sugerencia")
    public ResponseEntity<Void> enviar(@Valid @RequestBody SugerenciaRequest request) {
        service.enviar(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Listar todas las sugerencias (solo admins)")
    public ResponseEntity<List<SugerenciaResponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }
}
