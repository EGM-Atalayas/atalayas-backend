package com.atalayas.backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Vista de perfil del usuario autenticado (tabla usuario).
 *
 * Incluye nombreCompleto calculado para mostrar en la cabecera
 * y en la página de perfil sin necesitar concatenarlo en el frontend.
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
    private String nombreCompleto;
    private String avatarUrl;
    private String puestoTrabajo;
    private String departamento;

    // FK empresa
    private UUID empresaId;
    private String nombreEmpresa;

    // FK rol
    private UUID rolId;
    private String codigoRol;
    private String nombreRol;

    private String bannerUrl;
    private String bio;
    private String telefono;
    private com.atalayas.backend.user.enums.Disponibilidad disponibilidad;
    private boolean notifNuevoModulo;
    private boolean notifModuloCompletado;
    private boolean notifComunicado;
    private boolean notifPendiente;
    private boolean modoOscuro;

    private boolean activo;
    private boolean terminosAceptados;

    private OffsetDateTime fechaRegistro;
    private OffsetDateTime ultimoLogin;
    private OffsetDateTime actualizadoEn;
}