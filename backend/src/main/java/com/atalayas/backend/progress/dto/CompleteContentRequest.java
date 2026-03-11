package com.atalayas.backend.progress.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * Payload para registrar o actualizar la trazabilidad de lectura
 * de un usuario sobre un contenido (tabla trazabilidad_lectura).
 */
@Data
public class CompleteContentRequest {

    @NotNull(message = "El usuario es obligatorio")
    private UUID usuarioId;

    @NotNull(message = "El contenido es obligatorio")
    private UUID contenidoId;

    @NotNull(message = "La empresa es obligatoria")
    private UUID empresaId;

    @Min(value = 0, message = "El tiempo en segundos no puede ser negativo")
    private int tiempoSegundos;

    @Min(value = 1, message = "La versión leída debe ser al menos 1")
    private int versionLeida;

    /** Hash de aceptación / firma del usuario al completar el contenido. */
    private String hashAceptacion;

    private boolean completado;
}


