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

    // Endpoints públicos (Swagger protegido — eliminado de esta lista)
    public static final String[] PUBLIC_URLS = {
            "/api/v1/auth/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

}

