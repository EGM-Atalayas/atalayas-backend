package com.atalayas.backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Vista de perfil del usuario autenticado (tabla usuario).
 * Incluye nombre completo calculado para uso en UI.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private UUID usuarioId;
    private String email;
    private String nombre;
    private String apellidos;
    private String nombreCompleto;   // nombre + " " + apellidos
    private String avatarUrl;
    private String puestoTrabajo;

    // FK empresa
    private UUID empresaId;
    private String nombreEmpresa;

    // FK rol
    private UUID rolId;
    private String codigoRol;
    private String nombreRol;

    private boolean activo;
    private boolean terminosAceptados;

    private LocalDateTime fechaRegistro;
    private LocalDateTime ultimoLogin;
    private LocalDateTime actualizadoEn;
}
