package com.atalayas.backend.common.enums;

/**
 * Estado derivado para lógica interna de servicio.
 * Se construye a partir de los campos completado + tiempo_segundos
 * de la tabla trazabilidad_lectura; no se persiste directamente en BD.
 */
public enum ProgressStatus {

    /** El usuario aún no ha iniciado el contenido. */
    PENDIENTE,

    /** El usuario ha empezado pero no ha marcado como completado. */
    EN_PROGRESO,

    /** El usuario completó el contenido (completado = true). */
    COMPLETADO
}

