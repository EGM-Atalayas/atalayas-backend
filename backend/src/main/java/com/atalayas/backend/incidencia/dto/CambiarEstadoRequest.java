package com.atalayas.backend.incidencia.dto;

import com.atalayas.backend.incidencia.enums.EstadoIncidencia;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CambiarEstadoRequest {

    @NotNull(message = "El estado es obligatorio")
    private EstadoIncidencia estado;
}
