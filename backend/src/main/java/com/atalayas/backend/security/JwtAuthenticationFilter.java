package com.atalayas.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String jwt = resolveToken(request);

        // Sin token → dejar pasar; Spring Security protege los endpoints protegidos con 401
        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Token presente → validar. Cualquier fallo = 401 inmediato (no continuar sin auth)
        try {
            final String userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    log.warn("Token JWT expirado o inválido para usuario: {}", userEmail);
                    sendUnauthorized(response, "Token expirado o inválido. Usa /api/v1/auth/refresh-token para renovar la sesión.");
                    return;
                }
            }
        } catch (Exception e) {
            log.error("Token JWT malformado o no verificable: {}", e.getMessage());
            sendUnauthorized(response, "Token malformado o no verificable.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Resuelve el token JWT en el siguiente orden de prioridad:
     * 1. Header Authorization: Bearer <token>
     * 2. Cookie HttpOnly "accessToken"
     */
    private String resolveToken(HttpServletRequest request) {
        // 1. Header Authorization
        final String authHeader = request.getHeader(SecurityConstants.HEADER_STRING);
        if (authHeader != null && authHeader.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return authHeader.substring(SecurityConstants.TOKEN_PREFIX.length());
        }

        // 2. Cookie HttpOnly
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(c -> SecurityConstants.ACCESS_TOKEN_COOKIE.equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        return null;
    }

    /**
     * Escribe una respuesta 401 JSON estructurada y corta la cadena de filtros.
     * Se usa cuando el token existe pero es inválido o ha expirado.
     */
    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> body = new HashMap<>();
        body.put("status", 401);
        body.put("error", "No autorizado");
        body.put("message", message);
        new ObjectMapper().writeValue(response.getOutputStream(), body);
    }
}

