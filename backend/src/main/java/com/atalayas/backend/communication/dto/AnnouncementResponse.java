package com.atalayas.backend.communication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Respuesta con los datos de un anuncio (tabla anuncio)
 *
 * Los anuncios son comunicaciones de empresa a empleados.
 * Pueden ser globales (visibles para toda la plataforma) o
 * específicos de una empresa (visibles solo para sus empleados).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementResponse {

    private UUID anuncioId;

    // null cuando esGlobal = true, el anuncio no pertenece a ninguna empresa concreta
    private UUID empresaId;

    private String titulo;
    private String contenido;

    // true = visible para todos los usuarios de la plataforma
    private boolean esGlobal;

    private boolean activo;

    // ID del usuario que creó el anuncio
    private UUID creadoPor;

    // URL de imagen opcional para mostrar en la tarjeta del anuncio
    private String imagenUrl;

    private String enlaceUrl;
    private String enlaceTexto;
    private String videoUrl;
    private String adjuntoUrl;
    private String adjuntoNombre;

    /** 'publicado' | 'borrador' */
    private String estado;

    private boolean fijado;

    private int vistas;

    private OffsetDateTime creadoEn;
    private OffsetDateTime actualizadoEn;
}