package com.atalayas.backend.progress.dto;

import com.atalayas.backend.common.enums.ProgressStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Respuesta con el estado de trazabilidad de un usuario sobre un contenido
 * (tabla trazabilidad_lectura).
 * El campo {@code estado} es derivado en servicio a partir de
 * {@code completado} + {@code tiempoSegundos} usando {@link ProgressStatus}.
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

    // FK empresa
    private UUID empresaId;

    private boolean completado;
    private LocalDateTime fechaCompletado;

    private int tiempoSegundos;
    private int versionLeida;

    private String hashAceptacion;

    /** Estado derivado para lógica interna — NO persiste en BD. */
    private ProgressStatus estado;

    private LocalDateTime actualizadoEn;
}

