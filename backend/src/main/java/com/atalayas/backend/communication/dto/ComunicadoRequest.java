package com.atalayas.backend.communication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;


/**
 * Payload para crear un comunicado oficial de EGM
 * creadoPor se extrae del usuario autenticado, nunca del body
 * Solo accesible para ROLE_ADMIN
 */
@Data
public class ComunicadoRequest {

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 250, message = "El título no puede superar los 250 caracteres")
    private String titulo;

    @NotBlank(message = "El mensaje es obligatorio")
    private String mensaje;

    /** Opcional URL de imagen adjunta o decorativa */
    @Size(max = 500, message = "La URL de imagen no puede superar los 500 caracteres")
    private String imagenUrl;

    /**
     * Opcional, si no se envía, se usa now() en el @PrePersist
     * Permite programar la publicación para una fecha futura
     */
    private OffsetDateTime fechaPublicacion;

    /** Opcional, si no se envía, el comunicado no caduca automáticamente */
    private OffsetDateTime fechaExpiracion;
}