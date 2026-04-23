package com.atalayas.backend.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Resumen del dashboard para ROLE_ADMIN_EMPRESA.
 * Scope: datos de su propia empresa.
 */
@Getter
@Builder
public class AdminEmpresaResumenResponse {

    private String nombreEmpresa;
    private long usuariosActivos;
    private long usuariosInactivos;
}

