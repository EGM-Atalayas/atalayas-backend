package com.atalayas.backend.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Respuesta de un evento de comunidad
 * Se devuelve en creación, listado y actualización
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityEventResponse {

    private UUID eventoId;
    private UUID empresaId;
    private UUID creadoPor;
    private String titulo;
    private String descripcion;
    private boolean esGlobal;
    private boolean activo;
    private OffsetDateTime fechaInicio;
    private OffsetDateTime fechaFin;
    private OffsetDateTime creadoEn;
    private OffsetDateTime actualizadoEn;
}