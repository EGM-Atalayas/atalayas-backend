package com.atalayas.backend.incidencia.dto;

import com.atalayas.backend.incidencia.enums.EstadoIncidencia;
import com.atalayas.backend.incidencia.enums.PrioridadIncidencia;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidenciaResponse {

    private Long id;
    private String titulo;
    private String descripcion;
    private EstadoIncidencia estado;
    private PrioridadIncidencia prioridad;
    private UUID empresaId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private OffsetDateTime creadoEn;
}

