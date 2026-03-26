package com.atalayas.backend.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Resumen del dashboard para ROLE_ADMIN (superadmin EGM).
 * Scope: datos agregados de toda la plataforma.
 */
@Getter
@Builder
public class SuperAdminResumenResponse {

    private long totalEmpresas;
    private long empresasPendientes;
    private long empresasAprobadas;
    private long totalUsuarios;
}

