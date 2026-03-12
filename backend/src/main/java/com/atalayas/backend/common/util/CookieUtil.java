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
                .secure(secure)
                .path("/")
                .maxAge(maxAgeSec)
                .sameSite("Strict")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * Invalida una cookie poniéndole maxAge=0.
     */
    public static void clearCookie(HttpServletResponse response,
                                   String name,
                                   boolean secure) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}

