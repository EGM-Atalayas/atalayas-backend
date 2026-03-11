package com.atalayas.backend.module.dto;

import com.atalayas.backend.common.enums.ModuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Vista resumida de un módulo para listados y selectores.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleSummaryResponse {

    private UUID moduloId;
    private String nombre;
    private ModuleType tipoModulo;
    private Integer orden;
    private boolean esEspecializadoIa;
    private boolean activo;
}

