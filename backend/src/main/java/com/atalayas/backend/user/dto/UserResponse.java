package com.atalayas.backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Respuesta completa de un usuario (tabla usuario).
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
    private boolean activo;
    private boolean terminosAceptados;
    private int intentosFallidos;

    private LocalDateTime fechaRegistro;
    private LocalDateTime ultimoLogin;
    private LocalDateTime actualizadoEn;
}
