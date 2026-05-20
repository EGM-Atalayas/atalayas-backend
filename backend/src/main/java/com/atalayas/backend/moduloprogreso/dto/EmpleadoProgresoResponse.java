package com.atalayas.backend.moduloprogreso.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpleadoProgresoResponse {
    private UUID usuarioId;
    private String nombre;
    private String apellidos;
    private List<ModuloProgresoResumen> modulos;
}
