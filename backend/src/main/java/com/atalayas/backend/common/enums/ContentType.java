package com.atalayas.backend.common.enums;

/**
 * Tipos de contenido persistidos en la columna tipo_contenido de la tabla contenido.
 * Se usa con @Enumerated(EnumType.STRING).
 */
public enum ContentType {

    /** Recurso de vídeo (url_recurso apunta al vídeo). */
    VIDEO,

    /** Artículo o documento de texto (cuerpo_texto). */
    TEXTO,

    /** Documento PDF (url_recurso apunta al fichero). */
    PDF,

    /** Evaluación / cuestionario — tiene preguntas en contenido_pregunta. */
    EVALUACION,

    /** Contenido mixto generado por IA (texto + recursos). */
    IA_GENERADO
}

