package com.atalayas.backend.rewards.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Payload para crear o actualizar un beneficio
 */
@Data
public class BenefitRequest {

    @NotBlank(message = "El título es obligatorio")
    private String titulo;

    private String descripcion;

    private String urlInfo;

    private String iconoUrl;

    private String comoAcceder;

    private OffsetDateTime fechaFin;

    // null = beneficio global - solo superadmin puede crear globales
    private UUID empresaId;
}