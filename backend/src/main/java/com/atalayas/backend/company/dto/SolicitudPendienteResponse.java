package com.atalayas.backend.company.dto;

import com.atalayas.backend.common.enums.EstadoSolicitud;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Respuesta enriquecida para el listado de solicitudes pendientes.
 * Combina datos de la empresa con datos del usuario admin provisional.
 * Usada en GET /api/v1/empresas/solicitudes (ROLE_ADMIN).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudPendienteResponse {

    private UUID            empresaId;
    private String          nombreEmpresa;
    private String          cif;
    private String          emailContacto;
    private EstadoSolicitud estadoSolicitud;
    private OffsetDateTime  fechaSolicitud;

    /** Nombre completo del admin provisional (nombre + apellidos). */
    private String nombreAdmin;
    private String emailAdmin;
}
