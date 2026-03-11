package com.atalayas.backend.content.dto;

import com.atalayas.backend.common.enums.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * Payload para crear o actualizar un contenido (tabla contenido).
 */
@Data
public class ContentRequest {

    @NotNull(message = "El módulo es obligatorio")
    private UUID moduloId;

    /** Empresa propietaria del contenido (null = contenido global). */
    private UUID empresaId;

    @NotBlank(message = "El título es obligatorio")
    private String titulo;

    private String descripcion;

    @NotNull(message = "El tipo de contenido es obligatorio")
    private ContentType tipoContenido;

    private String urlRecurso;

    private String cuerpoTexto;

    private int orden = 0;

    private int version = 1;

    private Integer minutosEstimados;

    private boolean esIaGenerado = false;

    private boolean activo = true;
}

