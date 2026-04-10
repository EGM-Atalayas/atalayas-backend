package com.atalayas.backend.progress.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * Payload para registrar o actualizar la trazabilidad de lectura
 * de un usuario sobre un contenido (tabla trazabilidad_lectura)
 *
 * El frontend envía este payload cada vez que el empleado avanza en un contenido,
 * ya sea al cerrar el visor, al completar una sección o al marcar como terminado.
 */
@Data
public class CompleteContentRequest {

    @NotNull(message = "El usuario es obligatorio")
    private UUID usuarioId;

    @NotNull(message = "El contenido es obligatorio")
    private UUID contenidoId;

    @NotNull(message = "La empresa es obligatoria")
    private UUID empresaId;

    // Tiempo de la sesión actual en segundos
    @Min(value = 0, message = "El tiempo en segundos no puede ser negativo")
    private int tiempoSegundos;

    // Porcentaje de completado del contenido (0-100)
    @Min(value = 0, message = "El porcentaje no puede ser negativo")
    @Max(value = 100, message = "El porcentaje no puede superar 100")
    private int porcentajeCompletado;

    // Versión del contenido que está leyendo el empleado
    @Min(value = 1, message = "La versión leída debe ser al menos 1")
    private int versionLeida;

    // Hash de aceptación o firma digital al completar contenidos normativos
    private String hashAceptacion;

    // true cuando el empleado marca el contenido como terminado
    // Una vez marcado como completado no se puede revertir
    private boolean completado;
}