package com.atalayas.backend.auth.controller;

import com.atalayas.backend.auth.dto.AuthResponse;
import com.atalayas.backend.auth.dto.LoginRequest;
import com.atalayas.backend.auth.dto.RefreshTokenRequest;
import com.atalayas.backend.auth.dto.RegisterRequest;
import com.atalayas.backend.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*") // Permitir solicitudes desde cualquier origen (ajustar según necesidades)
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints de login, registro y refresh token")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar nuevo usuario")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión y obtener token JWT")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }




    @PostMapping("/refresh-token")
    @Operation(summary = "Refrescar el access token usando el refresh token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }
}


