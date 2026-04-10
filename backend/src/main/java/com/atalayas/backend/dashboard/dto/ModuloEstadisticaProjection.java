package com.atalayas.backend.dashboard.dto;

/**
 * Proyección Spring Data para la query nativa de estadísticas de módulos.
 * Los nombres de los getters deben coincidir exactamente con los alias SQL
 * (nombre, completados, pendientes).
 */
public interface ModuloEstadisticaProjection {
    String getNombre();
    Long getCompletados();
    Long getPendientes();
}

