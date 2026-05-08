package com.atalayas.backend.company.event;

import com.atalayas.backend.common.enums.EstadoSolicitud;

import java.util.UUID;

/**
 * Evento publicado por CompanyService tras persistir un cambio de estado relevante.
 * Se procesa en CompanyEventListener DESPUÉS del commit de BD (AFTER_COMMIT).
 *
 * Cubre dos transiciones que requieren envío de email:
 *   PENDIENTE → APROBADA  : email de bienvenida al admin de la empresa
 *   PENDIENTE → RECHAZADA : email de rechazo (datos capturados antes del hard delete)
 */
public record CompanyEvent(
        UUID empresaId,
        String nombreEmpresa,
        String emailAdmin,
        String nombreAdmin,
        EstadoSolicitud estadoAnterior,
        EstadoSolicitud estadoNuevo
) {}

