package com.atalayas.backend.communication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

/**
 * Payload para crear una notificación manual desde el panel de admin
 *
 * El campo leido no se acepta en creación, siempre arranca en false
 * Solo ROLE_ADMIN y ROLE_ADMIN_EMPRESA pueden crear notificaciones manuales
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {

    @NotNull(message = "El destinatario es obligatorio")
    private UUID destinatarioId;

    @NotBlank(message = "El tipo no puede estar vacío")
    @Size(max = 100, message = "El tipo no puede superar 100 caracteres")
    private String tipo;

    @NotBlank(message = "El mensaje no puede estar vacío")
    private String mensaje;

    // Opcional — enlace al recurso al que hace referencia la notificación
    @Size(max = 500, message = "El enlace no puede superar 500 caracteres")
    private String enlace;
}