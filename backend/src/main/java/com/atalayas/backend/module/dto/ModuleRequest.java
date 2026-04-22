package com.atalayas.backend.module.dto;

import com.atalayas.backend.common.enums.ModuleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * Payload para crear o actualizar un módulo (tabla modulo).
 */
@Data
public class ModuleRequest {

    @NotBlank(message = "El nombre del módulo es obligatorio")
    private String nombre;

    private String descripcion;

    /** Empresa propietaria del módulo (null = módulo global). */
    private UUID empresaId;

    @NotNull(message = "El tipo de módulo es obligatorio")
    private ModuleType tipoModulo;

    private Integer orden;

    private boolean esEspecializadoIa = false;

    private boolean activo = true;

    private String idioma;

    private String duracion;

    /** Audiencia: "todos" | "administradores" | "departamento" */
    private String audiencia;

    /** Departamentos destinatarios (cuando audiencia = "departamento") */
    private String[] departamentos;

    /** Preguntas del test serializado como JSON string */
    private String testPreguntas;
}

