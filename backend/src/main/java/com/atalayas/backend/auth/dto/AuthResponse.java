package com.atalayas.backend.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    // Access token expuesto en el body para soportar Authorization: Bearer
    // (necesario cuando las cookies cross-site son bloqueadas por el navegador)
    private String accessToken;
    private long expiresIn;

    // ── Datos del usuario autenticado (tabla usuario) ────────────────────────
    private UUID usuarioId;
    private String email;
    private String nombre;
    private String apellidos;
    private String avatarUrl;

    // ── Rol (tabla rol) ──────────────────────────────────────────────────────
    private UUID rolId;
    private String codigoRol;
    private String nombreRol;

    // ── Empresa (tabla empresa) ──────────────────────────────────────────────
    private UUID empresaId;
    private String nombreEmpresa;
}
