package com.atalayas.backend.company.controller;

import com.atalayas.backend.company.dto.CambioEstadoRequest;
import com.atalayas.backend.company.dto.CompanyResponse;
import com.atalayas.backend.company.dto.SolicitudAltaEmpresaRequest;
import com.atalayas.backend.company.dto.SolicitudAltaEmpresaResponse;
import com.atalayas.backend.company.service.CompanyService;
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
    @Operation(summary = "Solicitar alta de empresa",
               description = "Endpoint **público** — no requiere autenticación. Crea la empresa en estado `PENDIENTE` y su usuario administrador con `activo = false`. La cuenta se activa cuando el SUPER_ADMIN apruebe la solicitud.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Solicitud creada correctamente",
                     content = @Content(schema = @Schema(implementation = SolicitudAltaEmpresaResponse.class))),
        @ApiResponse(responseCode = "400", description = "CIF o email ya registrado / datos inválidos",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
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
                   Gestiona el ciclo de vida de una empresa. Transiciones válidas:

                   | Desde | Hacia | Efecto |
                   |---|---|---|
                   | `PENDIENTE` | `APROBADA` | Activa usuarios, email bienvenida, notificación interna |
                   | `PENDIENTE` | `RECHAZADA` | Desactiva empresa, email de rechazo |
                   | `RECHAZADA` | `PENDIENTE` | Reset sin efectos secundarios |
                   | `APROBADA` | cualquiera | **PROHIBIDO** — empresa ya operativa |
                   | `RECHAZADA` | `APROBADA` | **PROHIBIDO** — debe pasar antes por `PENDIENTE` |
                   """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estado actualizado correctamente",
                     content = @Content(schema = @Schema(implementation = CompanyResponse.class))),
        @ApiResponse(responseCode = "400", description = "Transición no permitida o empresa en mismo estado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "401", description = "Sin autenticación",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "403", description = "Rol insuficiente — requiere ROLE_ADMIN",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Empresa no encontrada",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<CompanyResponse> cambiarEstado(
            @PathVariable UUID id,
            @Valid @RequestBody CambioEstadoRequest request) {
        return ResponseEntity.ok(companyService.cambiarEstado(id, request));
    }
}

