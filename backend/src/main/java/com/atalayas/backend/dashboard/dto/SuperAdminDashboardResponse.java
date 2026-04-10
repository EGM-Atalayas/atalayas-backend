package com.atalayas.backend.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Respuesta completa de GET /api/v1/dashboard/superadmin.
 * Contiene métricas globales de la plataforma EGM Atalayas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminDashboardResponse {

    /** Total de empresas registradas en la plataforma (todas). */
    private long empresasAdheridas;

    /** Empresas cuya fechaSolicitud cae en el mes en curso. */
    private long empresasNuevasMes;

    /** Total de usuarios registrados en la plataforma. */
    private long empleadosRegistrados;

    /** Usuarios cuya fechaRegistro cae en el mes en curso. */
    private long empleadosNuevosMes;

    /** Módulos con activo = true. */
    private long modulosPublicados;

    /** Incidencias en estado ABIERTA. */
    private long incidenciasAbiertas;

    /** Incidencias en estado ABIERTA y prioridad CRITICA. */
    private long incidenciasCriticas;

    /** Hasta 10 eventos recientes ordenados de más nuevo a más antiguo. */
    private List<ActividadRecienteDto> actividadReciente;
}

