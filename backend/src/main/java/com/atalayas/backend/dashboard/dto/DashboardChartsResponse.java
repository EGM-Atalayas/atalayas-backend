package com.atalayas.backend.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Respuesta completa de GET /api/v1/dashboard/superadmin/graficas */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardChartsResponse {
    private List<EvolucionMensualDto> evolucion;
    private List<SectorDistribucionDto> sectores;
    private List<ModuloEstadisticaDto> modulos;
}

