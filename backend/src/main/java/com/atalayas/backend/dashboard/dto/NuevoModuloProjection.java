package com.atalayas.backend.dashboard.dto;

import java.time.OffsetDateTime;

/**
 * Proyección para eventos de tipo "nuevo":
 * módulo publicado reciente (propio de la empresa o global).
 */
public interface NuevoModuloProjection {
    String getNombreModulo();
    OffsetDateTime getFecha();
}

