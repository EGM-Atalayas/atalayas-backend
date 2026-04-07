package com.atalayas.backend.company.dto;

import com.atalayas.backend.common.enums.EstadoSolicitud;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Respuesta al POST /api/v1/empresas (solicitud pública de alta)
 *
 * Devuelve los datos de la empresa recién creada en estado PENDIENTE
 * y del usuario administrador provisional con activo=false,
 * que quedará inactivo hasta que EGM apruebe la solicitud
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudAltaEmpresaResponse {

    // ── EMPRESA ───────────────────────────────────────────────────────────
    private UUID empresaId;
    private String nombreEmpresa;
    private String cif;
    private String sector;
    private String emailContacto;
    private String telefonoContacto;
    private String descripcion;
    private EstadoSolicitud estadoSolicitud;
    private OffsetDateTime fechaSolicitud;

    // ── USUARIO ADMIN PROVISIONAL ─────────────────────────────────────────
    private UUID usuarioId;
    private String nombreAdmin;
    private String apellidosAdmin;
    private String emailAdmin;

    // false hasta que EGM apruebe la solicitud
    private boolean activoAdmin;
}