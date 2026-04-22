package com.atalayas.backend.common.enums;

/**
 * Estados posibles del campo estado_solicitud de la tabla empresa.
 * Coincide exactamente con el CHECK constraint de PostgreSQL.
 *
 * Transiciones válidas:
 *   PENDIENTE  → APROBADA  : aprobación desde /{id}/solicitud
 *   PENDIENTE  → (delete)  : rechazo desde /{id}/solicitud — hard delete de empresa y usuarios
 *   APROBADA   → PAUSADA   : suspensión temporal desde /{id}/estado
 *   PAUSADA    → APROBADA  : reactivación desde /{id}/estado
 */
public enum EstadoSolicitud {

    /** Solicitud de alta enviada, pendiente de revisión por SUPER_ADMIN. */
    PENDIENTE,

    /** Solicitud aprobada — la empresa está operativa en la plataforma. */
    APROBADA,

    /** Empresa aprobada pero suspendida temporalmente por el SUPER_ADMIN. */
    PAUSADA
}

