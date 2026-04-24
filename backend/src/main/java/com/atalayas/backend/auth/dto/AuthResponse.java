package com.atalayas.backend.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Respuesta que devuelve el backend tras login, register, refresh y /auth/me
 *
 * Contiene el access token (para soportar Authorization: Bearer cuando las cookies
 * cross-site son bloqueadas) y todos los datos del usuario que el frontend necesita
 * para construir el header, el perfil y los guards de navegación por rol.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    // Token de acceso, también va en cookie HttpOnly
    private String accessToken;
    private long expiresIn;

    // ── DATOS DEL USUARIO ──────────────────────────────────────────────────
    private UUID usuarioId;
    private String email;
    private String nombre;
    private String apellidos;
    private String avatarUrl;

    // Puesto de trabajo
    private String puestoTrabajo;

    // ── ROL ───────────────────────────────────────────────────────────────
    private UUID rolId;
    private String codigoRol;
    private String nombreRol;

    // ── EMPRESA ───────────────────────────────────────────────────────────
    private UUID empresaId;
    private String nombreEmpresa;

    // URL del logo de la empresa
    private String logoEmpresaUrl;
}
