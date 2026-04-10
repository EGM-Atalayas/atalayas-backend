package com.atalayas.backend.rewards.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Respuesta de un beneficio del área empresarial (tabla beneficio)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenefitResponse {

    private UUID beneficioId;
    private UUID empresaId;
    private UUID creadoPor;
    private String titulo;
    private String descripcion;
    private String urlInfo;
    private boolean activo;
    private OffsetDateTime creadoEn;
    private OffsetDateTime actualizadoEn;
}