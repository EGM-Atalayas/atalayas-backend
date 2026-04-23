package com.atalayas.backend.common.enums;

/**
 * Estados posibles del campo estado_solicitud de la tabla empresa.
 * Coincide exactamente con el CHECK constraint de PostgreSQL.
 *
 * Transiciones válidas:
 *   PENDIENTE  → APROBADA  : aprobación desde /{id}/solicitud
 *   PENDIENTE  → RECHAZADA : rechazo desde /{id}/solicitud — hard delete inmediato de empresa y usuarios
 *   APROBADA   → PAUSADA   : suspensión temporal desde /{id}/estado
 *   PAUSADA    → APROBADA  : reactivación desde /{id}/estado
 *
 * Nota: RECHAZADA es un estado transitorio — la empresa se elimina físicamente de la BD
 * tras el rechazo, por lo que este valor nunca persiste en la tabla empresa.
 */
public enum EstadoSolicitud {

    /** Solicitud de alta enviada, pendiente de revisión por SUPER_ADMIN. */
    PENDIENTE,

    /** Solicitud aprobada — la empresa está operativa en la plataforma. */
    APROBADA,

    /** Empresa aprobada pero suspendida temporalmente por el SUPER_ADMIN. */
    PAUSADA,

    /**
     * Solicitud rechazada por el SUPER_ADMIN.
     * Estado transitorio: al rechazar, la empresa y sus usuarios se eliminan
     * físicamente de la BD inmediatamente después (hard delete).
     */
    RECHAZADA
}
