package com.atalayas.backend.documento.enums;

/**
 * Tipos de documento que se pueden gestionar en la plataforma.
 *
 * - NOMINA       : Nómina mensual del empleado
 * - CONTRATO     : Contrato laboral, anexos
 * - CERTIFICADO  : Certificado de formación, asistencia, etc.
 * - POLITICA     : Política interna, manual de empresa
 * - OTRO         : Cualquier otro documento
 */
public enum TipoDocumento {
    NOMINA,
    CONTRATO,
    CERTIFICADO,
    POLITICA,
    OTRO
}
