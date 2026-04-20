package com.atalayas.backend.user.mapper;

import com.atalayas.backend.role.entity.Role;
import com.atalayas.backend.user.dto.UserProfileResponse;
import com.atalayas.backend.user.dto.UserResponse;
import com.atalayas.backend.user.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper para convertir entre la entidad User y sus DTOs.
 *
 * toUserResponse() — vista completa para uso administrativo.
 * toUserProfileResponse() — vista de perfil para el usuario autenticado,
 * incluye nombreCompleto calculado para mostrar en la UI.
 */
@Component
public class UserMapper {

    public UserResponse toUserResponse(User user) {
        Role role = user.getRol();
        return UserResponse.builder()
                .usuarioId(user.getUsuarioId())
                .email(user.getEmail())
                .nombre(user.getNombre())
                .apellidos(user.getApellidos())
                .avatarUrl(user.getAvatarUrl())
                .empresaId(user.getEmpresaId())
                .rolId(role.getRolId())
                .codigoRol(role.getCodigoRol())
                .nombreRol(role.getNombreRol())
                .puestoTrabajo(user.getPuestoTrabajo())
                .activo(user.isActivo())
                .terminosAceptados(user.isTerminosAceptados())
                .intentosFallidos(user.getIntentosFallidos())
                .fechaRegistro(user.getFechaRegistro())
                .ultimoLogin(user.getUltimoLogin())
                .actualizadoEn(user.getActualizadoEn())
                .build();
    }

    public UserProfileResponse toUserProfileResponse(User user) {
        Role role = user.getRol();
        return UserProfileResponse.builder()
                .usuarioId(user.getUsuarioId())
                .email(user.getEmail())
                .nombre(user.getNombre())
                .apellidos(user.getApellidos())
                .nombreCompleto(user.getNombreCompleto())
                .avatarUrl(user.getAvatarUrl())
                .bannerUrl(user.getBannerUrl())
                .bio(user.getBio())
                .telefono(user.getTelefono())
                .disponibilidad(user.getDisponibilidad())
                .notifNuevoModulo(user.isNotifNuevoModulo())
                .notifModuloCompletado(user.isNotifModuloCompletado())
                .notifComunicado(user.isNotifComunicado())
                .notifPendiente(user.isNotifPendiente())
                .modoOscuro(user.isModoOscuro())
                .puestoTrabajo(user.getPuestoTrabajo())
                .empresaId(user.getEmpresaId())
                .rolId(role.getRolId())
                .codigoRol(role.getCodigoRol())
                .nombreRol(role.getNombreRol())
                .activo(user.isActivo())
                .terminosAceptados(user.isTerminosAceptados())
                .fechaRegistro(user.getFechaRegistro())
                .ultimoLogin(user.getUltimoLogin())
                .actualizadoEn(user.getActualizadoEn())
                .build();
    }
}