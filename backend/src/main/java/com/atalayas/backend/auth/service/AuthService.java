package com.atalayas.backend.auth.service;

import com.atalayas.backend.auth.dto.AuthResponse;
import com.atalayas.backend.auth.dto.LoginRequest;
import com.atalayas.backend.auth.dto.RegisterRequest;
import com.atalayas.backend.common.util.SecurityUtils;
import com.atalayas.backend.communication.service.NotificationService;
import com.atalayas.backend.company.entity.Company;
import com.atalayas.backend.company.repository.CompanyRepository;
import com.atalayas.backend.role.entity.Role;
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

/**
 * Lógica de autenticación, login, registro, refresh y consulta de sesión activa
 *
 * Todos los métodos que devuelven AuthResponse incluyen los datos de empresa
 * (nombre y logo) para que el frontend pueda construir el header sin
 * necesitar una segunda llamada al backend.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final NotificationService notificationService;


    // ── REGISTRO ──────────────────────────────────────────────────────────
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                    "Ya existe un usuario con el email: " + request.getEmail());
        }

        Role role = roleRepository.findById(request.getRolId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Rol no encontrado con id: " + request.getRolId()));

        User user = User.builder()
                .nombre(request.getNombre())
                .apellidos(request.getApellidos())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .empresaId(request.getEmpresaId())
                .rol(role)
                .puestoTrabajo(request.getPuestoTrabajo())
                .build();

        user = userRepository.save(user);

        // Notificación de bienvenida solo si el usuario quedó activo.
        // Los usuarios creados vía solicitud de empresa arrancan con activo=false
        // y recibirán su bienvenida cuando CompanyService los active al aprobar.
        if (user.isActivo()) {
            notificationService.crearInterna(
                    user.getUsuarioId(),
                    "BIENVENIDA",
                    "¡Bienvenido/a " + user.getNombre()
                            + "! Explora tus módulos formativos.",
                    "/dashboard"
            );
        }

        return buildAuthResponse(user);
    }


    // ── LOGIN ─────────────────────────────────────────────────────────────

    public AuthResponse login(LoginRequest request) {
        // Spring Security valida credenciales — lanza excepción si son incorrectas
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario no encontrado"));

        return buildAuthResponse(user);
    }


    // ── REFRESH TOKEN ─────────────────────────────────────────────────────

    /**
     * Valida el refresh token leído desde la cookie y devuelve los datos del usuario.
     * El controller es quien genera los nuevos tokens y los escribe en las cookies.
     */
    public AuthResponse refreshToken(String rawRefreshToken) {
        final String email = jwtService.extractUsername(rawRefreshToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario no encontrado"));

        if (!jwtService.isTokenValid(rawRefreshToken, user)) {
            throw new IllegalArgumentException("Refresh token inválido o expirado");
        }

        return buildAuthResponse(user);
    }


    // ── SESIÓN ACTIVA ─────────────────────────────────────────────────────

    /**
     * Devuelve los datos del usuario autenticado en el contexto de seguridad.
     * Lo usa el endpoint GET /auth/me para que el frontend verifique la sesión.
     */
    @Transactional(readOnly = true)
    public AuthResponse getCurrentUserInfo() {
        User user = SecurityUtils.getCurrentUser();
        return userRepository.findByEmail(user.getEmail())
                .map(this::buildAuthResponse)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario autenticado no encontrado en BD"));
    }


    // ── GENERACIÓN DE TOKENS ──────────────────────────────────────────────

    /**
     * Genera un par [accessToken, refreshToken] para el usuario.
     * Lo usa el controller para escribir las cookies HttpOnly.
     */
    public String[] generateTokenPair(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario no encontrado"));
        return new String[]{
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user)
        };
    }


    // ── MAPPER INTERNO ────────────────────────────────────────────────────
    /**
     * Construye el AuthResponse con todos los datos del usuario y su empresa.
     * Hacemos una sola consulta a Company para obtener nombre y logo —
     * así el frontend no necesita una segunda llamada para construir el header.
     */
    private AuthResponse buildAuthResponse(User user) {
        Role role = user.getRol();

        // Cargamos los datos de la empresa si el usuario tiene una asignada.
        // El superadmin EGM puede no tener empresa propia, por eso es Optional.
        String nombreEmpresa = null;
        String logoEmpresaUrl = null;

        if (user.getEmpresaId() != null) {
            Company empresa = companyRepository.findById(user.getEmpresaId())
                    .orElse(null);
            if (empresa != null) {
                nombreEmpresa = empresa.getNombreEmpresa();
                logoEmpresaUrl = empresa.getLogoUrl();
            }
        }

        return AuthResponse.builder()
                .expiresIn(SecurityConstants.ACCESS_TOKEN_EXPIRATION)
                .usuarioId(user.getUsuarioId())
                .email(user.getEmail())
                .nombre(user.getNombre())
                .apellidos(user.getApellidos())
                .avatarUrl(user.getAvatarUrl())
                .bannerUrl(user.getBannerUrl())
                .puestoTrabajo(user.getPuestoTrabajo())
                .rolId(role.getRolId())
                .codigoRol(role.getCodigoRol())
                .nombreRol(role.getNombreRol())
                .empresaId(user.getEmpresaId())
                .nombreEmpresa(nombreEmpresa)
                .logoEmpresaUrl(logoEmpresaUrl)
                .build();
    }
}