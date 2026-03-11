package com.atalayas.backend.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

/**
 * Payload para crear o actualizar una empresa (tabla empresa).
 */
@Data
public class CompanyRequest {

    @NotBlank(message = "El nombre de la empresa es obligatorio")
    @Size(max = 255)
    private String nombreEmpresa;

    @Size(max = 255)
    private String razonSocial;

    @NotBlank(message = "El CIF es obligatorio")
    @Size(max = 20)
    private String cif;

    @NotNull(message = "El sector es obligatorio")
    private UUID sectorId;

    @Size(max = 500)
    private String logoUrl;

    private String mision;
    private String vision;
    private String valores;
    private String descripcionIa;

    /** Contexto industrial en formato JSON libre (JSONB en BD). */
    private IndustrialContextDto contextoIndustrial;

    private boolean esEgm = false;
}

