package com.atalayas.backend.dashboard.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Proyección para eventos individuales de progreso:
 * completado, inicio y logro (100% módulo).
 */
public interface ProgressEventProjection {
    UUID getUsuarioId();
    UUID getModuloId();
    String getNombreUsuario();
    String getNombreModulo();
    OffsetDateTime getFecha();
}

