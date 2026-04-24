package com.atalayas.backend.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Evento de actividad reciente del dashboard de admin empresa.
 * tipo: "completado" | "inicio" | "logro" | "grupo" | "nuevo"
 * timestamp: ISO 8601 — el frontend calcula el tiempo relativo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActividadItemDto {

    private String tipo;
    private String texto;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private OffsetDateTime timestamp;
}

