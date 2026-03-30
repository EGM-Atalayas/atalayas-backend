package com.atalayas.backend.communication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload para crear un anuncio (tabla anuncio).
 * empresaId y creadoPor se obtienen del usuario autenticado, nunca del body.
 */
@Data
public class AnnouncementRequest {

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 255, message = "El título no puede superar los 255 caracteres")
    private String titulo;

    @NotBlank(message = "El contenido es obligatorio")
    private String contenido;

    /**
     * Solo ROLE_ADMIN puede crear con esGlobal=true.
     * Si el usuario es ROLE_ADMIN_EMPRESA este campo se ignora y se fuerza a false.
     */
    private boolean esGlobal = false;
}
