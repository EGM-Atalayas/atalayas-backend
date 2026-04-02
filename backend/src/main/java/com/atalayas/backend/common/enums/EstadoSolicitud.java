package com.atalayas.backend.common.enums;

/**
 * Estados posibles del campo estado_solicitud de la tabla empresa.
 * Coincide exactamente con el CHECK constraint de PostgreSQL.
 */
public enum EstadoSolicitud {

    /** Solicitud de alta enviada, pendiente de revisión por SUPER_ADMIN. */
    PENDIENTE,

    /** Solicitud aprobada — la empresa está operativa en la plataforma. */
    APROBADA,

    /** Solicitud rechazada por SUPER_ADMIN. */
    RECHAZADA
}

