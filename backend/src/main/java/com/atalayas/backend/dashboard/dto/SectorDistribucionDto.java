package com.atalayas.backend.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Distribución de empresas por sector para el gráfico de tarta. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectorDistribucionDto {
    private String name;
    private long value;
}

