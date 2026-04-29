package com.atalayas.backend.auth.service;

import com.atalayas.backend.auth.dto.AuthResponse;
import com.atalayas.backend.auth.dto.LoginRequest;
import com.atalayas.backend.auth.dto.RegisterRequest;
import com.atalayas.backend.role.entity.Rol;
import com.atalayas.backend.role.repository.RoleRepository;
import com.atalayas.backend.security.JwtService;
import com.atalayas.backend.security.SecurityConstants;
import com.atalayas.backend.user.entity.User;
import com.atalayas.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Ya existe un usuario con el email: " + request.getEmail());
        }

        Rol rol = roleRepository.findById(request.getRolId())
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado con id: " + request.getRolId()));

        User user = User.builder()
                .nombre(request.getNombre())
                .apellidos(request.getApellidos())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .empresaId(request.getEmpresaId())
                .rol(rol)
                .puestoTrabajo(request.getPuestoTrabajo())
                .build();

        userRepository.save(user);
        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return buildAuthResponse(user);
    }

    /**
     * Valida el refresh token (leído desde la cookie) y devuelve los datos del usuario.
     * El controller es quien genera los nuevos tokens y los escribe en las cookies.
     */
    public AuthResponse refreshToken(String rawRefreshToken) {
        final String email = jwtService.extractUsername(rawRefreshToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (!jwtService.isTokenValid(rawRefreshToken, user)) {
            throw new IllegalArgumentException("Refresh token inválido o expirado");
        }

        return buildAuthResponse(user);
    }

    /**
     * Genera un par [accessToken, refreshToken] para el usuario autenticado.
     * Lo usa el controller para escribir las cookies HttpOnly.
     */
    public String[] generateTokenPair(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        return new String[]{
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user)
        };
    }

    private AuthResponse buildAuthResponse(User user) {
        Rol rol = user.getRol();
        return AuthResponse.builder()
                .expiresIn(SecurityConstants.ACCESS_TOKEN_EXPIRATION)
                .usuarioId(user.getUsuarioId())
                .email(user.getEmail())
                .nombre(user.getNombre())
                .apellidos(user.getApellidos())
                .avatarUrl(user.getAvatarUrl())
                .rolId(rol.getRolId())
                .codigoRol(rol.getCodigoRol())
                .nombreRol(rol.getNombreRol())
                .empresaId(user.getEmpresaId())
                .build();
    }

    /**
     * Devuelve los datos del usuario autenticado en el SecurityContext.
     * Usado por GET /api/v1/auth/me.
     */
    public AuthResponse getCurrentUserInfo() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder
                        .getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            throw new IllegalStateException("No hay sesión activa");
        }
        return buildAuthResponse(user);
    }
}
