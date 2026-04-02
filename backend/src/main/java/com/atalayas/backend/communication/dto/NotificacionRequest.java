package com.atalayas.backend.communication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;


/**
 * Payload para crear una notificación manual
 * destinatarioId debe venir siempre en el body
 * leido no se acepta en creación — siempre arranca en false
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificacionRequest {

    @NotNull(message = "El destinatario es obligatorio")
    private UUID destinatarioId;

    @NotBlank(message = "El tipo no puede estar vacío")
    @Size(max = 100, message = "El tipo no puede superar 100 caracteres")
    private String tipo;

    @NotBlank(message = "El mensaje no puede estar vacío")
    private String mensaje;

    @Size(max = 500, message = "El enlace no puede superar 500 caracteres")
    private String enlace;
}