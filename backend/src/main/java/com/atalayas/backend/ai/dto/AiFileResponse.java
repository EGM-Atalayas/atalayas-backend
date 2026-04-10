package com.atalayas.backend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Respuesta del endpoint POST /api/v1/ai/generar-desde-archivo.
 * Contiene el contenido formativo generado a partir de un archivo subido.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiFileResponse {
    private String titulo;
    private String descripcion;
    private String contenido;
    private String modelo;
    private OffsetDateTime generadoEn;
}
