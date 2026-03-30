package com.atalayas.backend.communication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Respuesta con los datos de un anuncio (tabla anuncio).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementResponse {

    private UUID anuncioId;

    /** null cuando esGlobal = true */
    private UUID empresaId;

    private String titulo;
    private String contenido;
    private boolean esGlobal;
    private boolean activo;

    /** ID del usuario que creó el anuncio */
    private UUID creadoPor;

    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;
}
