package com.atalayas.backend.communication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Respuesta con los datos de un comunicado (tabla comunicado).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementResponse {

    private UUID comunicadoId;

    // FK empresa (opcional)
    private UUID empresaId;
    private String nombreEmpresa;

    // FK usuario creador (opcional)
    private UUID creadoPor;
    private String nombreCreador;

    private String titulo;
    private String mensaje;

    private LocalDateTime fechaPublicacion;
    private LocalDateTime fechaExpiracion;
    private LocalDateTime actualizadoEn;
}

