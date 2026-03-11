package com.atalayas.backend.content.dto;

import com.atalayas.backend.common.enums.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Respuesta completa de un contenido (tabla contenido).
 * Incluye las preguntas asociadas de la tabla contenido_pregunta.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentResponse {

    private UUID contenidoId;

    // FK módulo
    private UUID moduloId;
    private String nombreModulo;

    // FK empresa (opcional)
    private UUID empresaId;
    private String nombreEmpresa;

    private String titulo;
    private String descripcion;
    private ContentType tipoContenido;
    private String urlRecurso;
    private String cuerpoTexto;

    private int orden;
    private int version;
    private Integer minutosEstimados;
    private boolean esIaGenerado;
    private boolean activo;

    /** Preguntas asociadas (tabla contenido_pregunta). */
    private List<QuestionResponse> preguntas;

    private LocalDateTime fechaCreacion;
    private LocalDateTime actualizadoEn;
}

