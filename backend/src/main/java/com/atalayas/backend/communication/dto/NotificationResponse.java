package com.atalayas.backend.communication.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Respuesta con los datos de una notificación
 * Se devuelve en creación, listado y marcado de lectura
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private UUID notificacionId;
    private UUID destinatarioId;
    private String tipo;
    private String mensaje;
    private String enlace;
    private boolean leido;
    private OffsetDateTime creadoEn;
    private OffsetDateTime actualizadoEn;
}