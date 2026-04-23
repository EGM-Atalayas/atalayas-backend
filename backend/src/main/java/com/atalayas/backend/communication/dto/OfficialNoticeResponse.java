package com.atalayas.backend.communication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Respuesta con los datos de un comunicado oficial de EGM
 * Se devuelve en creación, listado y desactivación
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfficialNoticeResponse {

    private UUID comunicadoId;
    private UUID creadoPor;
    private String titulo;
    private String mensaje;
    private String imagenUrl;
    private OffsetDateTime fechaPublicacion;
    private OffsetDateTime fechaExpiracion;
    private boolean activo;
    private OffsetDateTime actualizadoEn;
}