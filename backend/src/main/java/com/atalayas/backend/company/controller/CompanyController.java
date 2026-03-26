package com.atalayas.backend.company.controller;

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

    @PatchMapping("/{id}/aprobar")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Aprobar solicitud de empresa (SUPER_ADMIN) — activa el usuario admin y envía email")
    public ResponseEntity<CompanyResponse> aprobar(@PathVariable UUID id) {
        return ResponseEntity.ok(companyService.aprobar(id));
    }

    @PatchMapping("/{id}/rechazar")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Rechazar solicitud de empresa (SUPER_ADMIN) — el usuario queda inactivo en BD y recibe email")
    public ResponseEntity<CompanyResponse> rechazar(@PathVariable UUID id) {
        return ResponseEntity.ok(companyService.rechazar(id));
    }
}

