package com.atalayas.backend.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Elemento de la línea de tiempo del dashboard del superadmin.
 * Campo {@code tipo}: "info" | "success" | "warning" | "error"
 * Campo {@code tiempo}: cadena relativa calculada en memoria ("hace 2h", "hace 5m"…)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActividadRecienteDto {
    private Long   id;
    private String texto;
    private String tiempo;
    private String tipo;
}

