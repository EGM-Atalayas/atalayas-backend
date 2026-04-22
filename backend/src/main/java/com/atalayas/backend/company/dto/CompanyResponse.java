package com.atalayas.backend.company.dto;

import com.atalayas.backend.common.enums.EstadoSolicitud;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Respuesta con los datos completos de una empresa (tabla empresa).
 * Se devuelve en listados y en la resolución de solicitudes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyResponse {

    private UUID empresaId;
    private String nombreEmpresa;
    private String cif;
    private String sector;
    private String logoUrl;
    private String emailContacto;
    private String telefonoContacto;
    private String descripcion;
    private EstadoSolicitud estadoSolicitud;
    private boolean activo;
    private OffsetDateTime fechaSolicitud;
    private OffsetDateTime fechaResolucion;
    private OffsetDateTime actualizadoEn;

    /** Datos del administrador provisional — solo se rellenan en GET /empresas (getAll). */
    private String nombre;
    private String apellidos;
    private String emailAdmin;
}