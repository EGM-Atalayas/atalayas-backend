package com.atalayas.backend.user.service;

import com.atalayas.backend.common.util.SecurityUtils;
import com.atalayas.backend.exception.ResourceNotFoundException;
import com.atalayas.backend.user.dto.UserProfileResponse;
import com.atalayas.backend.user.dto.UserResponse;
import com.atalayas.backend.user.entity.User;
import com.atalayas.backend.user.mapper.UserMapper;
import com.atalayas.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

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
}
