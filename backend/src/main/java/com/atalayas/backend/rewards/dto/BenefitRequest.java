package com.atalayas.backend.rewards.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

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

    // null = beneficio global - solo superadmin puede crear globales
    private UUID empresaId;
}