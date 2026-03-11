package com.atalayas.backend.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * Payload para crear o actualizar una pregunta (tabla contenido_pregunta).
 */
@Data
public class QuestionRequest {

    @NotNull(message = "El contenido es obligatorio")
    private UUID contenidoId;

    @NotBlank(message = "El enunciado es obligatorio")
    private String enunciado;

    /** Respuesta correcta (puede ser null en preguntas abiertas). */
    private String respuestaCorrecta;
}

