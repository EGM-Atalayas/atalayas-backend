package com.atalayas.backend.auth.controller;

import com.atalayas.backend.auth.dto.AuthResponse;
import com.atalayas.backend.auth.dto.ForgotPasswordRequest;
import com.atalayas.backend.auth.dto.LoginRequest;
import com.atalayas.backend.auth.dto.RegisterRequest;
import com.atalayas.backend.auth.dto.ResetPasswordRequest;
import com.atalayas.backend.auth.service.AuthService;
import com.atalayas.backend.auth.service.PasswordResetService;
import com.atalayas.backend.common.util.CookieUtil;
import com.atalayas.backend.security.SecurityConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    private final PasswordResetService passwordResetService;

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
    @Operation(summary = "Registrar nuevo usuario",
               description = "Crea un nuevo usuario empleado en la empresa indicada. Devuelve los tokens en cookies HttpOnly y en el body.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuario registrado correctamente",
                     content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Email ya registrado o datos inválidos",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "404", description = "Empresa no encontrada",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletResponse response) {
        AuthResponse body = authService.register(request);
        String accessToken = writeAuthCookies(response, body.getEmail());
        body.setAccessToken(accessToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión",
               description = "Autentica al usuario y devuelve los tokens en cookies HttpOnly (`accessToken`, `refreshToken`) y también en el body. El campo `accessToken` del body puede usarse como Bearer token.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login correcto — cookies y token en el body",
                     content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "Credenciales incorrectas",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
        @ApiResponse(responseCode = "400", description = "Usuario inactivo / empresa no aprobada",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletResponse response) {
        AuthResponse body = authService.login(request);
        String accessToken = writeAuthCookies(response, body.getEmail());
        body.setAccessToken(accessToken);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refrescar el access token",
               description = "Usa la cookie `refreshToken` (HttpOnly) para emitir un nuevo par de tokens. El refresh token expira a los **7 días**.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tokens renovados correctamente"),
        @ApiResponse(responseCode = "400", description = "Cookie refreshToken no encontrada o inválida",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
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
    @Operation(summary = "Cerrar sesión",
               description = "Limpia las cookies `accessToken` y `refreshToken`. No requiere token en el body.")
    @ApiResponse(responseCode = "200", description = "Sesión cerrada. Respuesta: `{ \"message\": \"Sesión cerrada correctamente\" }`")
    public ResponseEntity<Map<String, String>> logout(HttpServletResponse response) {
        CookieUtil.clearCookie(response, SecurityConstants.ACCESS_TOKEN_COOKIE, cookieSecure);
        CookieUtil.clearCookie(response, SecurityConstants.REFRESH_TOKEN_COOKIE, cookieSecure);
        return ResponseEntity.ok(Map.of("message", "Sesión cerrada correctamente"));
    }

    @GetMapping("/me")
    @Operation(summary = "Datos del usuario autenticado",
               description = "Devuelve el perfil del usuario actualmente autenticado junto con el `accessToken` activo. "
                           + "Útil para restaurar la sesión tras una recarga de página. "
                           + "Requiere `accessToken` válido (cookie o Bearer).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Datos del usuario + accessToken activo",
                     content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "Sin sesión activa o token expirado",
                     content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<AuthResponse> me(HttpServletRequest request) {
        AuthResponse body = authService.getCurrentUserInfo();

        // Devolver el token activo que llegó en la request (cookie o Bearer)
        // para que el frontend pueda restaurar la sesión completa tras una recarga.
        String token = resolveToken(request);
        body.setAccessToken(token);

        return ResponseEntity.ok(body);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Solicitar recuperación de contraseña",
               description = "Envía un email con un enlace para restablecer la contraseña. Siempre devuelve 200 para no revelar si el email existe.")
    @ApiResponse(responseCode = "200", description = "Solicitud procesada")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.solicitarRecuperacion(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "Si el correo está registrado, recibirás un enlace en breve"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Restablecer contraseña",
               description = "Establece una nueva contraseña usando el token recibido por email.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Contraseña restablecida correctamente"),
        @ApiResponse(responseCode = "400", description = "Token inválido, expirado o ya usado")
    })
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.restablecerPassword(request.getToken(), request.getNuevaPassword());
        return ResponseEntity.ok(Map.of("message", "Contraseña restablecida correctamente"));
    }

    /**
     * Extrae el JWT de la request en el mismo orden que JwtAuthenticationFilter:
     * 1. Header Authorization: Bearer <token>
     * 2. Cookie HttpOnly "accessToken"
     */
    private String resolveToken(HttpServletRequest request) {
        final String authHeader = request.getHeader(SecurityConstants.HEADER_STRING);
        if (authHeader != null && authHeader.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return authHeader.substring(SecurityConstants.TOKEN_PREFIX.length());
        }
        return extractCookie(request, SecurityConstants.ACCESS_TOKEN_COOKIE).orElse(null);
    }
}
