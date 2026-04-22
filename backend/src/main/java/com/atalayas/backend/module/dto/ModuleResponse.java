package com.atalayas.backend.module.dto;

import com.atalayas.backend.common.enums.ModuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Respuesta completa de un módulo formativo (tabla modulo).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleResponse {

    private UUID moduloId;
    private String nombre;
    private String descripcion;

    // FK empresa — null si es módulo global
    private UUID empresaId;
    private String nombreEmpresa;

    private ModuleType tipoModulo;
    private Integer orden;
    private boolean esEspecializadoIa;
    private boolean activo;

    private String idioma;
    private String duracion;
    private String audiencia;
    private String departamentos;
    private String testPreguntas;
    private String imagenPortadaUrl;
    private String tiposSalida;
    private String scriptPodcast;
    private String scriptVideo;
    private String podcastAudioUrl;

    private OffsetDateTime fechaCreacion;
    private OffsetDateTime actualizadoEn;
}