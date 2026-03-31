package com.atalayas.backend.rewards.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Respuesta de un beneficio del área empresarial
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
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;
}