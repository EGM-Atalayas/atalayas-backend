package com.atalayas.backend.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Respuesta de una pregunta (tabla contenido_pregunta).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {

    private UUID preguntaId;
    private UUID contenidoId;
    private String enunciado;
    private String respuestaCorrecta;
    private LocalDateTime actualizadoEn;
}

