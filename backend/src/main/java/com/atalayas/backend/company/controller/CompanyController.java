package com.atalayas.backend.company.controller;

import com.atalayas.backend.company.dto.CambioEstadoRequest;
import com.atalayas.backend.company.dto.CompanyResponse;
import com.atalayas.backend.company.dto.SolicitudAltaEmpresaRequest;
import com.atalayas.backend.company.dto.SolicitudAltaEmpresaResponse;
import com.atalayas.backend.company.service.CompanyService;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/empresas")
@RequiredArgsConstructor
@Tag(name = "Empresas", description = "Gestión del ciclo de vida de empresas en la plataforma")
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping("/solicitud")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Solicitar alta de empresa — público, sin autenticación previa. Crea la empresa (PENDIENTE) y su usuario admin (inactivo)")
    public ResponseEntity<SolicitudAltaEmpresaResponse> crear(@Valid @RequestBody SolicitudAltaEmpresaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyService.crearEmpresa(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Listar todas las empresas (SUPER_ADMIN)")
    public ResponseEntity<List<CompanyResponse>> getAll() {
        return ResponseEntity.ok(companyService.getAll());
    }

    @GetMapping("/pendientes")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Listar empresas pendientes de resolución (SUPER_ADMIN)")
    public ResponseEntity<List<CompanyResponse>> getPendientes() {
        return ResponseEntity.ok(companyService.getPendientes());
    }

    @GetMapping("/aprobadas")
    @Operation(summary = "Listar empresas aprobadas — público, usado en el selector de registro")
    public ResponseEntity<List<CompanyResponse>> getAprobadas() {
        return ResponseEntity.ok(companyService.getAprobadas());
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cambiar estado de empresa (SUPER_ADMIN)",
               description = """
                   Transiciones válidas:
                   - PENDIENTE → APROBADA: activa usuarios, envía email de bienvenida y notificación interna
                   - PENDIENTE → RECHAZADA: envía email de rechazo, desactiva empresa
                   - RECHAZADA → PENDIENTE: reset sin efectos secundarios
                   - APROBADA → cualquier estado: PROHIBIDO
                   - RECHAZADA → APROBADA: PROHIBIDO (debe pasar antes por PENDIENTE)
                   """)
    public ResponseEntity<CompanyResponse> cambiarEstado(
            @PathVariable UUID id,
            @Valid @RequestBody CambioEstadoRequest request) {
        return ResponseEntity.ok(companyService.cambiarEstado(id, request));
    }
}

