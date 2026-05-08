package com.atalayas.backend.common.enums;

/**
 * Tipos de módulo persistidos en la columna tipo_modulo de la tabla modulo.
 * Se usa con @Enumerated(EnumType.STRING).
 */
public enum ModuleType {

    // ── Legacy (compatibilidad con datos históricos) ──────────────────────
    /** @deprecated Usar FORMACION_BASICA o FORMACION_ESPECIFICA */
    GENERAL,
    /** @deprecated Usar FORMACION_ESPECIFICA */
    ESPECIALIZADO,
    /** Módulo generado o asistido por IA. */
    ESPECIALIZADO_IA,

    // ── Valores activos ───────────────────────────────────────────────────
    IDENTIDAD_CORPORATIVA,
    FORMACION_BASICA,
    FORMACION_ESPECIFICA,
    DESARROLLO_PROFESIONAL,
    RECOMPENSAS_VENTAJAS,
    COMUNIDAD,
    CUMPLIMIENTO,
    LIDERAZGO,
    TECNICO,
    SOFT_SKILLS,
    ONBOARDING
}

