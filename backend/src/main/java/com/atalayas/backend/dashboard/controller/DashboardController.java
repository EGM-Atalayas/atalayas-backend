package com.atalayas.backend.dashboard.controller;

import com.atalayas.backend.dashboard.dto.AdminEmpresaResumenResponse;
import com.atalayas.backend.dashboard.dto.DashboardChartsResponse;
import com.atalayas.backend.dashboard.dto.SuperAdminDashboardResponse;
import com.atalayas.backend.dashboard.dto.SuperAdminResumenResponse;
import com.atalayas.backend.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
     */
    @GetMapping("/admin/resumen")
    @PreAuthorize("hasAuthority('ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Resumen del dashboard para el admin de empresa")
    public ResponseEntity<AdminEmpresaResumenResponse> getAdminEmpresaResumen() {
        return ResponseEntity.ok(dashboardService.getAdminEmpresaResumen());
    }

    /**
     * GET /api/v1/dashboard/superadmin/resumen
     * Solo accesible para ROLE_ADMIN (superadmin EGM). Endpoint legado.
     */
    @GetMapping("/superadmin/resumen")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Resumen legado del dashboard para el superadmin EGM")
    public ResponseEntity<SuperAdminResumenResponse> getSuperAdminResumen() {
        return ResponseEntity.ok(dashboardService.getSuperAdminResumen());
    }

    /**
     * GET /api/v1/dashboard/superadmin
     * Dashboard completo del superadmin EGM con todas las métricas y actividad reciente.
     * - Sin sesión     → 401
     * - Rol incorrecto → 403
     */
    @GetMapping("/superadmin")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(
        summary = "Dashboard principal del superadmin EGM",
        description = """
            Devuelve métricas globales de la plataforma:
            empresas, empleados, módulos publicados, incidencias abiertas y actividad reciente.
            El campo `tipo` de cada actividad es uno de: `info`, `success`, `warning`, `error`.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Métricas devueltas correctamente"),
        @ApiResponse(responseCode = "401", description = "Sin sesión activa",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "403", description = "Rol insuficiente — requiere ROLE_ADMIN",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<SuperAdminDashboardResponse> getSuperAdminDashboard() {
        return ResponseEntity.ok(dashboardService.getSuperAdminDashboard());
    }

    /**
     * GET /api/v1/dashboard/superadmin/graficas
     * Datos para los tres gráficos del dashboard del superadmin.
     */
    @GetMapping("/superadmin/graficas")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(
        summary = "Datos de gráficos para el dashboard del superadmin",
        description = """
            Devuelve tres conjuntos de datos listos para renderizar directamente en el frontend:

            - **evolucion**: totales acumulados de empresas y empleados al final de cada uno \
            de los últimos 6 meses. Ideal para un gráfico de líneas.
            - **sectores**: distribución de empresas agrupadas por sector. \
            Ideal para un gráfico de tarta.
            - **modulos**: top 10 módulos activos con sus conteos de contenidos \
            completados y pendientes. Ideal para un gráfico de barras.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Datos de gráficos devueltos correctamente",
                     content = @Content(schema = @Schema(implementation = DashboardChartsResponse.class))),
        @ApiResponse(responseCode = "401", description = "Sin sesión activa",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "403", description = "Rol insuficiente — requiere ROLE_ADMIN",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<DashboardChartsResponse> getDashboardCharts() {
        return ResponseEntity.ok(dashboardService.getDashboardCharts());
    }
}
