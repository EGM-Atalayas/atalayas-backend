package com.atalayas.backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Respuesta completa de un usuario (tabla usuario).
 * Usada en listados administrativos y consultas por ID.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID usuarioId;
    private String email;
    private String nombre;
    private String apellidos;
    private String avatarUrl;

    // FK empresa
    private UUID empresaId;
    private String nombreEmpresa;

    // FK rol
    private UUID rolId;
    private String codigoRol;
    private String nombreRol;

    private String puestoTrabajo;
    private String departamento;
    private boolean activo;
    private boolean terminosAceptados;
    private int intentosFallidos;

    private OffsetDateTime fechaRegistro;
    private OffsetDateTime fechaBaja;
    private OffsetDateTime ultimoLogin;
    private OffsetDateTime actualizadoEn;
}
