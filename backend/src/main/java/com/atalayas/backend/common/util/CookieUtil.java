package com.atalayas.backend.common.util;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;

public final class CookieUtil {

    private CookieUtil() {}

    /**
     * Emite una cookie HttpOnly con el token JWT.
     *
     * En producción (secure=true) añade manualmente el atributo {@code Partitioned} (CHIPS)
     * porque Spring Boot 3.2 no lo soporta via API. Sin él, Chrome y Firefox bloquean
     * la cookie como tercero en contextos cross-site (Vercel ↔ Render), causando 401.
     *
     * @param response   respuesta HTTP donde se añade la cookie
     * @param name       nombre de la cookie
     * @param value      valor (el token)
     * @param maxAgeSec  tiempo de vida en segundos
     * @param secure     true → solo HTTPS + SameSite=None + Partitioned (producción)
     */
    public static void addTokenCookie(HttpServletResponse response,
                                      String name,
                                      String value,
                                      long maxAgeSec,
                                      boolean secure) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(maxAgeSec)
                .sameSite(secure ? "None" : "Lax")
                .build();

        // En producción añadimos Partitioned manualmente (CHIPS).
        // Necesario para que Chrome y Firefox no bloqueen la cookie cross-site.
        String cookieHeader = secure
                ? cookie.toString() + "; Partitioned"
                : cookie.toString();

        response.addHeader("Set-Cookie", cookieHeader);
    }

    /**
     * Invalida una cookie poniéndole maxAge=0.
     * Aplica los mismos flags que addTokenCookie para que el navegador la elimine.
     */
    public static void clearCookie(HttpServletResponse response,
                                   String name,
                                   boolean secure) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(0)
                .sameSite(secure ? "None" : "Lax")
                .build();

        String cookieHeader = secure
                ? cookie.toString() + "; Partitioned"
                : cookie.toString();

        response.addHeader("Set-Cookie", cookieHeader);
    }
}
