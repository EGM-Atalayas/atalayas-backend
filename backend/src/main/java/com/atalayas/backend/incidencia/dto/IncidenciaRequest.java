package com.atalayas.backend.incidencia.dto;

import com.atalayas.backend.incidencia.enums.PrioridadIncidencia;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class IncidenciaRequest {

    @NotBlank(message = "El título es obligatorio")
    private String titulo;

    private String descripcion;

    private PrioridadIncidencia prioridad;

    /** Opcional: asocia la incidencia a una empresa concreta. null = global. */
    private UUID empresaId;
}

