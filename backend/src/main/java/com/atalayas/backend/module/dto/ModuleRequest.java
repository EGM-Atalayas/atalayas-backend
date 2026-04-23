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

    /** Departamentos destinatarios como JSON string (cuando audiencia = "departamento") */
    private String departamentos;

    /** Preguntas del test serializado como JSON string */
    private String testPreguntas;

    /** URL pública de la imagen de portada (Supabase Storage) */
    private String imagenPortadaUrl;

    /** Tipos de salida generados: "documentacion", "podcast", "video" (separados por coma) */
    private String tiposSalida;

    /** Guion de podcast generado por IA */
    private String scriptPodcast;

    /** Guion de video (JSON de slides) generado por IA */
    private String scriptVideo;

    /** Contenido formativo en Markdown (generado por IA o escrito manualmente) */
    private String contenidoMarkdown;

    /** URL pública del MP3 del podcast (Supabase Storage) */
    private String podcastAudioUrl;
}

