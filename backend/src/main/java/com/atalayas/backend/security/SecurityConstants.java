package com.atalayas.backend.security;

public final class SecurityConstants {

    private SecurityConstants() {}

    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";
    public static final long ACCESS_TOKEN_EXPIRATION = 1000L * 60 * 60;         // 1 hora
    public static final long REFRESH_TOKEN_EXPIRATION = 1000L * 60 * 60 * 24 * 7; // 7 días

    // Nombres de las cookies HttpOnly
    public static final String ACCESS_TOKEN_COOKIE  = "accessToken";
    public static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    // Endpoints públicos — rutas explícitas para no exponer /me ni futuros endpoints protegidos
    public static final String[] PUBLIC_URLS = {
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh-token",
            "/api/v1/auth/logout",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

}

