package com.atalayas.backend.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiPromptRequest {

    // Para chat y resumen
    @Size(max = 8000, message = "El prompt no puede superar 8000 caracteres")
    private String prompt;

    // Para generación de contenido formativo
    private String tema;
    private String tipoModulo;
    private String descripcion;

    // Para el chatbot - contexto de la empresa
    private String nombreEmpresa;
    private String contexto;

    // Para evaluaciones
    private Integer numPreguntas;
}