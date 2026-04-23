package com.atalayas.backend.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Un punto de la línea de evolución mensual de empresas y empleados. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvolucionMensualDto {
    /** Abreviatura del mes en español: "Ene", "Feb", … */
    private String mes;
    /** Total acumulado de empresas registradas hasta el final de ese mes. */
    private long empresas;
    /** Total acumulado de empleados registrados hasta el final de ese mes. */
    private long empleados;
}

