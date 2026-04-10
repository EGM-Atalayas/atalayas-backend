package com.atalayas.backend.common.util;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;

public final class CookieUtil {

    private CookieUtil() {}

    /**
     * Emite una cookie HttpOnly con el token JWT.
     *
     * @param response   respuesta HTTP donde se añade la cookie
     * @param name       nombre de la cookie
     * @param value      valor (el token)
     * @param maxAgeSec  tiempo de vida en segundos
     * @param secure     true → solo HTTPS (activar en producción)
     */
    public static void addTokenCookie(HttpServletResponse response,
                                      String name,
                                      String value,
                                      long maxAgeSec,
                                      boolean secure) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)                   // usar el parámetro (true en prod/HTTPS, false en dev/HTTP)
                .path("/")
                .maxAge(maxAgeSec)
                .sameSite(secure ? "None" : "Lax") // SameSite=None requiere Secure=true; en dev usamos Lax
                .build();

        // Partitioned (CHIPS) — requerido por Chrome para cookies cross-site con SameSite=None.
        // Afecta a despliegues donde frontend y backend están en subdominios distintos de railway.app.
        // ResponseCookie.partitioned() solo existe desde Spring Framework 6.4 (Boot 3.4+),
        // por lo que lo añadimos manualmente al header Set-Cookie.
        String cookieHeader = secure ? cookie.toString() + "; Partitioned" : cookie.toString();
        response.addHeader("Set-Cookie", cookieHeader);
    }

    /**
     * Invalida una cookie poniéndole maxAge=0.
     */
    public static void clearCookie(HttpServletResponse response,
                                   String name,
                                   boolean secure) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)                   // usar el parámetro
                .path("/")
                .maxAge(0)
                .sameSite(secure ? "None" : "Lax") // consistente con addTokenCookie
                .build();

        String cookieHeader = secure ? cookie.toString() + "; Partitioned" : cookie.toString();
        response.addHeader("Set-Cookie", cookieHeader);
    }
}
