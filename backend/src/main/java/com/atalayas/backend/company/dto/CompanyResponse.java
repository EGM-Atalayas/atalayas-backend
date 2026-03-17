package com.atalayas.backend.company.dto;

import com.atalayas.backend.common.enums.EstadoSolicitud;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Respuesta con datos completos de una empresa (tabla empresa).
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
    private String emailContacto;
    private String telefonoContacto;
    private String descripcion;
    private EstadoSolicitud estadoSolicitud;
    private boolean activo;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaResolucion;
    private LocalDateTime actualizadoEn;
}



