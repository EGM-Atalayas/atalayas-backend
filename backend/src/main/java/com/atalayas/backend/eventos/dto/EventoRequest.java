package com.atalayas.backend.eventos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class EventoRequest {

    @NotBlank(message = "El título es obligatorio")
    private String titulo;

    private String descripcion;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String lugar;
    private String urlInfo;
    private String imagenUrl;
}
