package com.atalayas.backend.progress.dto;

import com.atalayas.backend.common.enums.ProgressStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Respuesta con el estado de trazabilidad de un usuario sobre un contenido
 * Mapeado desde la tabla 'trazabilidad_lectura'
 *
 * El campo 'estado' es derivado en el servicio a partir de
 * 'completado' y 'tiempoSegundos'.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressResponse {

    private UUID registroId;

    // FK usuario
    private UUID usuarioId;
    private String nombreUsuario;

    // FK contenido
    private UUID contenidoId;
    private String tituloContenido;

    // FK módulo
    private UUID moduloId;

    // FK empresa
    private UUID empresaId;

    private boolean completado;
    private OffsetDateTime fechaCompletado;

    private int tiempoSegundos;

    // Porcentaje de completado del contenido (0-100)
    private int porcentajeCompletado;

    private int versionLeida;
    private String hashAceptacion;

    // Estado derivado para lógica de UI
    private ProgressStatus estado;

    private OffsetDateTime actualizadoEn;
}