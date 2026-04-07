package com.atalayas.backend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Respuesta estándar de cualquier operación de IA
 * Incluye el contenido generado, el modelo usado y el momento de generación
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiResponse {
    private String contenido;
    private String modelo;
    private OffsetDateTime generadoEn;
}