package com.atalayas.backend.company.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Respuesta con datos de una empresa (tabla empresa).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyResponse {

    private UUID empresaId;
    private String nombreEmpresa;
    private String razonSocial;
    private String cif;

    // FK sector
    private UUID sectorId;
    private String nombreSector;

    private String logoUrl;
    private String mision;
    private String vision;
    private String valores;
    private String descripcionIa;

    /** Contexto industrial en formato JSON libre (JSONB en BD). */
    private IndustrialContextDto contextoIndustrial;

    private boolean esEgm;

    private LocalDateTime fechaCreacion;
    private LocalDateTime actualizadoEn;
}

