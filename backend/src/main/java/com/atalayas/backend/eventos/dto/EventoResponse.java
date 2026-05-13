package com.atalayas.backend.eventos.dto;

import com.atalayas.backend.eventos.enums.EstadoEvento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoResponse {

    private UUID eventoId;
    private String titulo;
    private String descripcion;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String lugar;
    private String urlInfo;
    private String imagenUrl;
    private EstadoEvento estado;
    private UUID creadoPor;
    private boolean activo;
    private OffsetDateTime creadoEn;
    private OffsetDateTime actualizadoEn;
}
