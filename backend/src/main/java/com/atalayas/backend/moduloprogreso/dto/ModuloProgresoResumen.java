package com.atalayas.backend.moduloprogreso.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModuloProgresoResumen {
    private UUID moduloId;
    private String nombreModulo;
    private int porcentaje;
}
