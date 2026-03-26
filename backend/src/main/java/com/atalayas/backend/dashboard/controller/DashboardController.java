package com.atalayas.backend.dashboard.controller;

import com.atalayas.backend.dashboard.dto.AdminEmpresaResumenResponse;
import com.atalayas.backend.dashboard.dto.SuperAdminResumenResponse;
import com.atalayas.backend.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Resúmenes de métricas por rol")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * GET /api/v1/dashboard/admin/resumen
     * Solo accesible para ROLE_ADMIN_EMPRESA.
     * - Sin sesión        → 401 (AuthEntryPointJwt)
     * - Rol incorrecto    → 403 (GlobalExceptionHandler)
     * - Empresa no existe → 404 (ResourceNotFoundException)
     */
    @GetMapping("/admin/resumen")
    @PreAuthorize("hasAuthority('ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Resumen del dashboard para el admin de empresa")
    public ResponseEntity<AdminEmpresaResumenResponse> getAdminEmpresaResumen() {
        return ResponseEntity.ok(dashboardService.getAdminEmpresaResumen());
    }

    /**
     * GET /api/v1/dashboard/superadmin/resumen
     * Solo accesible para ROLE_ADMIN (superadmin EGM).
     * - Sin sesión        → 401 (AuthEntryPointJwt)
     * - Rol incorrecto    → 403 (GlobalExceptionHandler)
     */
    @GetMapping("/superadmin/resumen")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Resumen del dashboard para el superadmin EGM — datos agregados de toda la plataforma")
    public ResponseEntity<SuperAdminResumenResponse> getSuperAdminResumen() {
        return ResponseEntity.ok(dashboardService.getSuperAdminResumen());
    }
}

