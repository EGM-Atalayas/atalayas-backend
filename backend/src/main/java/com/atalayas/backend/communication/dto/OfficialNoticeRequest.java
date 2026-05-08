package com.atalayas.backend.communication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * Payload para crear un comunicado oficial de EGM
 *
 * El campo creadoPor se extrae siempre del
 * usuario autenticado en el servicio
 * Solo accesible para ROLE_ADMIN
 */
@Data
public class OfficialNoticeRequest {

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 250, message = "El título no puede superar los 250 caracteres")
    private String titulo;

    @NotBlank(message = "El mensaje es obligatorio")
    private String mensaje;

    // Opcional
    @Size(max = 500, message = "La URL de imagen no puede superar los 500 caracteres")
    private String imagenUrl;

    // Opcional
    private String categoria;

    // Opcional
    private Boolean destacado;

    // 'publicado' | 'borrador'
    private String estado;

    // Recursos opcionales
    @Size(max = 500)
    private String enlaceUrl;

    @Size(max = 200)
    private String enlaceTexto;

    @Size(max = 500)
    private String videoUrl;

    @Size(max = 500)
    private String adjuntoUrl;

    @Size(max = 200)
    private String adjuntoNombre;

    // Opcional
    private OffsetDateTime fechaPublicacion;

    // Opcional
    private OffsetDateTime fechaExpiracion;
}