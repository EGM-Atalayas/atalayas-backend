package com.atalayas.backend.user;

import com.atalayas.backend.common.enums.RoleType;
import com.atalayas.backend.common.service.ImageService;
import com.atalayas.backend.common.util.SecurityUtils;
import com.atalayas.backend.communication.service.EmailService;
import com.atalayas.backend.communication.service.NotificationService;
import com.atalayas.backend.exception.ResourceNotFoundException;
import com.atalayas.backend.role.entity.Rol;
import com.atalayas.backend.role.repository.RoleRepository;
import com.atalayas.backend.user.dto.ChangePasswordRequest;
import com.atalayas.backend.user.dto.CreateUserRequest;
import com.atalayas.backend.user.dto.UpdateProfileRequest;
import com.atalayas.backend.user.dto.UpdateUserRequest;
import com.atalayas.backend.user.dto.UserProfileResponse;
import com.atalayas.backend.user.dto.UserResponse;
import com.atalayas.backend.user.entity.User;
import com.atalayas.backend.user.mapper.UserMapper;
import com.atalayas.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 🔒 Servicio filtrado por empresaId.
 * 👑 SUPER_ADMIN bypassa el filtro y puede acceder a usuarios de cualquier empresa.
 *
 * <p>Regla de enmascarado: si un ADMIN/EMPLEADO accede al ID de un usuario
 * de otra empresa, se lanza {@link ResourceNotFoundException} (HTTP 404)
 * en lugar de 403, para no revelar que el recurso existe.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final ImageService imageService;

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile() {
        String email = SecurityUtils.getCurrentUser().getEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
        return userMapper.toUserProfileResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User user;
        if (SecurityUtils.isSuperAdmin()) {
            user = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        } else {
            UUID empresaId = SecurityUtils.getEmpresaId();
            // Si el id existe pero es de otra empresa, devolvemos el mismo 404
            // para no revelar que el recurso existe (enmascarado).
            user = userRepository.findByUsuarioIdAndEmpresaId(id, empresaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        }
        return userMapper.toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        if (SecurityUtils.isSuperAdmin()) {
            return userRepository.findAll().stream()
                    .map(userMapper::toUserResponse)
                    .collect(Collectors.toList());
        }
        return userRepository.findAllByEmpresaId(SecurityUtils.getEmpresaId()).stream()
                .map(userMapper::toUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void desactivarUsuario(UUID id) {
        User user;
        if (SecurityUtils.isSuperAdmin()) {
            user = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        } else {
            UUID empresaId = SecurityUtils.getEmpresaId();
            user = userRepository.findByUsuarioIdAndEmpresaId(id, empresaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        }
        user.setActivo(false);
        userRepository.save(user);
    }

    @Transactional
    public void activarUsuario(UUID id) {
        User user;
        if (SecurityUtils.isSuperAdmin()) {
            user = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        } else {
            UUID empresaId = SecurityUtils.getEmpresaId();
            user = userRepository.findByUsuarioIdAndEmpresaId(id, empresaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        }
        user.setActivo(true);
        userRepository.save(user);
    }

    /**
     * Crea un usuario desde el panel de administración.
     * Reglas de seguridad:
     * - ROLE_ADMIN_EMPRESA: crea usuarios solo en su propia empresa.
     *   No puede asignar ROLE_ADMIN.
     * - ROLE_ADMIN: puede crear usuarios en cualquier empresa (empresaId requerido).
     *   Puede asignar cualquier rol.
     */
    @Transactional
    public UserResponse crearUsuario(CreateUserRequest request) {
        // 1. Email único
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Ya existe un usuario con el email: " + request.getEmail());
        }

        // 2. Determinar empresaId según el rol del admin autenticado
        UUID empresaId;
        if (SecurityUtils.isSuperAdmin()) {
            if (request.getEmpresaId() == null) {
                throw new IllegalArgumentException("Debes especificar una empresa para el nuevo usuario");
            }
            empresaId = request.getEmpresaId();
        } else {
            // ROLE_ADMIN_EMPRESA: siempre usa su propia empresa (ignoramos empresaId del request)
            empresaId = SecurityUtils.getEmpresaId();
        }

        // 3. Buscar rol
        Rol role = roleRepository.findById(request.getRolId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con id: " + request.getRolId()));

        // 4. ROLE_ADMIN_EMPRESA no puede crear usuarios con ROLE_ADMIN
        if (!SecurityUtils.isSuperAdmin() && role.getRoleType() == RoleType.ROLE_ADMIN) {
            throw new AccessDeniedException("No tienes permisos para crear usuarios con el rol ROLE_ADMIN");
        }

        // 5. Crear y persistir el usuario
        User user = User.builder()
                .nombre(request.getNombre())
                .apellidos(request.getApellidos())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .empresaId(empresaId)
                .rol(role)
                .puestoTrabajo(request.getPuestoTrabajo())
                .departamento(request.getDepartamento())
                .build();

        final User savedUser = userRepository.save(user);
        log.info("Usuario creado por admin - usuarioId={} empresaId={} rol={}",
                savedUser.getUsuarioId(), empresaId, role.getCodigoRol());

        // 6. Notificación interna de bienvenida al nuevo usuario
        notificationService.crearInterna(
                savedUser.getUsuarioId(),
                "BIENVENIDA",
                "¡Bienvenido/a " + savedUser.getNombre() + "! Explora tus módulos formativos.",
                "/dashboard"
        );

        // 6b. Notificar al/los admin empresa que hay un nuevo empleado
        if (role.getRoleType() == RoleType.ROLE_EMPLEADO) {
            userRepository.findAllByEmpresaIdAndActivoTrue(empresaId).stream()
                    .filter(u -> "ROLE_ADMIN_EMPRESA".equals(u.getRol().getCodigoRol())
                            && !u.getUsuarioId().equals(savedUser.getUsuarioId()))
                    .forEach(admin -> notificationService.crearInterna(
                            admin.getUsuarioId(),
                            "EMPLEADO_NUEVO",
                            savedUser.getNombre() + " " + savedUser.getApellidos() + " se ha unido a tu empresa.",
                            "/dashboard/admin"
                    ));
        }

        // 7. Email de bienvenida — el fallo de email no revierte la creación
        try {
            emailService.enviarBienvenidaUsuarioCreado(
                    savedUser.getEmail(), savedUser.getNombre(), empresaId.toString());
        } catch (Exception e) {
            log.warn("No se pudo enviar el email de bienvenida a {}: {}", savedUser.getEmail(), e.getMessage());
        }

        return userMapper.toUserResponse(savedUser);
    }

    @Transactional
    public String uploadAvatar(MultipartFile file) {
        String url = imageService.processAndSaveAvatar(file);
        User user = getCurrentUser();
        user.setAvatarUrl(url);
        userRepository.save(user);
        return url;
    }

    @Transactional
    public UserProfileResponse updateMyProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();
        if (request.getNombre() != null) user.setNombre(request.getNombre());
        if (request.getApellidos() != null) user.setApellidos(request.getApellidos());
        if (request.getPuestoTrabajo() != null) user.setPuestoTrabajo(request.getPuestoTrabajo());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        if (request.getBannerUrl() != null) user.setBannerUrl(request.getBannerUrl());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getTelefono() != null) user.setTelefono(request.getTelefono());
        if (request.getDisponibilidad() != null) user.setDisponibilidad(request.getDisponibilidad());
        if (request.getNotifNuevoModulo() != null) user.setNotifNuevoModulo(request.getNotifNuevoModulo());
        if (request.getNotifModuloCompletado() != null) user.setNotifModuloCompletado(request.getNotifModuloCompletado());
        if (request.getNotifComunicado() != null) user.setNotifComunicado(request.getNotifComunicado());
        if (request.getNotifPendiente() != null) user.setNotifPendiente(request.getNotifPendiente());
        if (request.getModoOscuro() != null) user.setModoOscuro(request.getModoOscuro());
        return userMapper.toUserProfileResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user;
        if (SecurityUtils.isSuperAdmin()) {
            user = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        } else {
            UUID empresaId = SecurityUtils.getEmpresaId();
            user = userRepository.findByUsuarioIdAndEmpresaId(id, empresaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        }

        if (request.getNombre() != null) user.setNombre(request.getNombre());
        if (request.getApellidos() != null) user.setApellidos(request.getApellidos());
        if (request.getEmail() != null) {
            if (!request.getEmail().equals(user.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Ya existe un usuario con el email: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPuestoTrabajo() != null) {
            user.setPuestoTrabajo(request.getPuestoTrabajo().orElse(null));
        }
        if (request.getDepartamento() != null) {
            user.setDepartamento(request.getDepartamento().orElse(null));
        }

        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Transactional
    public void changeMyPassword(ChangePasswordRequest request) {
        if (!request.getPasswordNueva().equals(request.getPasswordConfirmar())) {
            throw new com.atalayas.backend.exception.BusinessException("Las contraseñas no coinciden");
        }
        User user = getCurrentUser();
        if (!passwordEncoder.matches(request.getPasswordActual(), user.getPassword())) {
            throw new com.atalayas.backend.exception.BusinessException("La contraseña actual es incorrecta");
        }
        user.setPassword(passwordEncoder.encode(request.getPasswordNueva()));
        userRepository.save(user);
    }

    private User getCurrentUser() {
        String email = SecurityUtils.getCurrentUser().getEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }
}
