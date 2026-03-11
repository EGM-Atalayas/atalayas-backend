package com.atalayas.backend.communication.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payload para crear o actualizar un comunicado (tabla comunicado).
 */
@Data
public class AnnouncementRequest {

    /** Empresa destinataria del comunicado (null = todas las empresas). */
    private UUID empresaId;

    /** Usuario que crea el comunicado (null = sistema). */
    private UUID creadoPor;

    @NotBlank(message = "El título es obligatorio")
    private String titulo;

    @NotBlank(message = "El mensaje es obligatorio")
    private String mensaje;

    /** Fecha a partir de la cual el comunicado es visible. Por defecto NOW() en BD. */
    private LocalDateTime fechaPublicacion;

    /** Fecha límite de visibilidad (null = sin expiración). */
    private LocalDateTime fechaExpiracion;
}

