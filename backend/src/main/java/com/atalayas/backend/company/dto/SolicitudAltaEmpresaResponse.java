package com.atalayas.backend.company.dto;

import com.atalayas.backend.common.enums.EstadoSolicitud;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Respuesta al POST /api/v1/empresas.
 * Incluye los datos de la empresa creada (PENDIENTE) y del usuario administrador
 * provisional (activo=false) que espera la aprobación del SUPER_ADMIN.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudAltaEmpresaResponse {

    // ── Empresa ──────────────────────────────────────────────────────────────
    private UUID empresaId;
    private String nombreEmpresa;
    private String cif;
    private String sector;
    private String emailContacto;
    private String telefonoContacto;
    private String descripcion;
    private EstadoSolicitud estadoSolicitud;
    private LocalDateTime fechaSolicitud;

    // ── Usuario admin provisional ────────────────────────────────────────────
    private UUID usuarioId;
    private String nombreAdmin;
    private String apellidosAdmin;
    private String emailAdmin;

    /** false hasta que el SUPER_ADMIN apruebe la solicitud. */
    private boolean activoAdmin;
}

