package com.atalayas.backend.dashboard.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Proyección para eventos de tipo "grupo":
 * ≥ 2 empleados completaron el mismo módulo el mismo día.
 */
public interface GrupoProjection {
    UUID getModuloId();
    String getNombreModulo();
    Long getCantidad();
    OffsetDateTime getFecha();
}

