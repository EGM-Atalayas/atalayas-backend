package com.atalayas.backend.moduloprogreso.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Body del endpoint POST /modulos/{moduloId}/progreso.
 * El frontend envía cuántos contenidos lleva completados y el total
 * del módulo. El servidor calcula el porcentaje y el flag completado.
 */
@Getter
@Setter
public class GuardarProgresoRequest {

    @NotNull
    @Min(0)
    private Integer contenidosCompletados;

    @NotNull
    @Min(0)
    private Integer totalContenidos;
}
