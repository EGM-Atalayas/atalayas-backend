package com.atalayas.backend.auth.controller;

import com.atalayas.backend.auth.dto.AuthResponse;
import com.atalayas.backend.auth.dto.LoginRequest;
import com.atalayas.backend.auth.dto.RegisterRequest;
import com.atalayas.backend.auth.service.AuthService;
import com.atalayas.backend.common.util.CookieUtil;
import com.atalayas.backend.security.SecurityConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints de login, registro, refresh token y logout")
public class AuthController {

    private final AuthService authService;

    /** En dev puedes poner app.cookie.secure=false en application.properties */
    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    // ── Helpers ────────────────────────────────────────────────────────────

    /**
     * Genera el par de tokens, escribe las cookies HttpOnly y devuelve el
     * accessToken para que también pueda incluirse en el body de la respuesta.
     * Así el frontend puede usarlo como Authorization: Bearer cuando el
     * navegador bloquee cookies cross-site (ej. Chrome con localhost ↔ railway.app).
     */
    private String writeAuthCookies(HttpServletResponse response, String email) {
        String[] tokens = authService.generateTokenPair(email);
        CookieUtil.addTokenCookie(response,
                SecurityConstants.ACCESS_TOKEN_COOKIE,
                tokens[0],
                SecurityConstants.ACCESS_TOKEN_EXPIRATION / 1000,
                cookieSecure);
        CookieUtil.addTokenCookie(response,
                SecurityConstants.REFRESH_TOKEN_COOKIE,
                tokens[1],
                SecurityConstants.REFRESH_TOKEN_EXPIRATION / 1000,
                cookieSecure);
        return tokens[0]; // accessToken
    }

    private Optional<String> extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    // ── Endpoints ──────────────────────────────────────────────────────────

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar nuevo usuario")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletResponse response) {
        AuthResponse body = authService.register(request);
        String accessToken = writeAuthCookies(response, body.getEmail());
        body.setAccessToken(accessToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión — tokens enviados en cookies HttpOnly y en el body")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletResponse response) {
        AuthResponse body = authService.login(request);
        String accessToken = writeAuthCookies(response, body.getEmail());
        body.setAccessToken(accessToken);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refrescar el access token usando la cookie refreshToken")
    public ResponseEntity<AuthResponse> refreshToken(HttpServletRequest request,
                                                     HttpServletResponse response) {
        String rawRefreshToken = extractCookie(request, SecurityConstants.REFRESH_TOKEN_COOKIE)
                .orElseThrow(() -> new IllegalArgumentException("Cookie refreshToken no encontrada"));

        AuthResponse body = authService.refreshToken(rawRefreshToken);
        String accessToken = writeAuthCookies(response, body.getEmail());
        body.setAccessToken(accessToken);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión — invalida las cookies de tokens")
    public ResponseEntity<Map<String, String>> logout(HttpServletResponse response) {
        CookieUtil.clearCookie(response, SecurityConstants.ACCESS_TOKEN_COOKIE, cookieSecure);
        CookieUtil.clearCookie(response, SecurityConstants.REFRESH_TOKEN_COOKIE, cookieSecure);
        return ResponseEntity.ok(Map.of("message", "Sesión cerrada correctamente"));
    }

    @GetMapping("/me")
    @Operation(summary = "Obtener datos del usuario autenticado (requiere accessToken)")
    public ResponseEntity<AuthResponse> me() {
        return ResponseEntity.ok(authService.getCurrentUserInfo());
    }
}
