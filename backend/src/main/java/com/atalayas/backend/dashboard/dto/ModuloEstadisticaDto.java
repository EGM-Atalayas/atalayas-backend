package com.atalayas.backend.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Estadísticas de completado de un módulo para el gráfico de barras. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModuloEstadisticaDto {
    private String nombre;
    private long completados;
    private long pendientes;
}

