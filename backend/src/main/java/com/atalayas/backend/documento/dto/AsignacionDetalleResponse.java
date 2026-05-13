package com.atalayas.backend.documento.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Detalle de la asignación de un documento a un empleado. Usado por
 * el admin para ver el estado por empleado.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsignacionDetalleResponse {
    private UUID asignacionId;
    private UUID usuarioId;
    private String nombre;
    private String apellidos;
    private String departamento;
    private OffsetDateTime fechaAsignacion;
    private boolean visto;
    private OffsetDateTime fechaVisto;
    private boolean firmado;
    private OffsetDateTime fechaFirma;
}
