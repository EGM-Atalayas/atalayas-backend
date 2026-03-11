package com.atalayas.backend.common.enums;

/**
 * Tipos de módulo persistidos en la columna tipo_modulo de la tabla modulo.
 * Se usa con @Enumerated(EnumType.STRING).
 */
public enum ModuleType {

    /** Módulo de formación general, disponible para todas las empresas. */
    GENERAL,

    /** Módulo especializado asociado a una empresa concreta. */
    ESPECIALIZADO,

    /** Módulo generado o asistido por IA (es_especializado_ia = true). */
    ESPECIALIZADO_IA,

    /** Módulo de cumplimiento normativo. */
    CUMPLIMIENTO,

    /** Módulo de onboarding para nuevas incorporaciones. */
    ONBOARDING
}

