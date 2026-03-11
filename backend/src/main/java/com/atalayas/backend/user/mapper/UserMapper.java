package com.atalayas.backend.user.mapper;

import com.atalayas.backend.role.entity.Rol;
import com.atalayas.backend.user.dto.UserProfileResponse;
import com.atalayas.backend.user.dto.UserResponse;
import com.atalayas.backend.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toUserResponse(User user) {
        Rol rol = user.getRol();
        return UserResponse.builder()
                .usuarioId(user.getUsuarioId())
                .email(user.getEmail())
                .nombre(user.getNombre())
                .apellidos(user.getApellidos())
                .avatarUrl(user.getAvatarUrl())
                .empresaId(user.getEmpresaId())
                .rolId(rol.getRolId())
                .codigoRol(rol.getCodigoRol())
                .nombreRol(rol.getNombreRol())
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
        Rol rol = user.getRol();
        return UserProfileResponse.builder()
                .usuarioId(user.getUsuarioId())
                .email(user.getEmail())
                .nombre(user.getNombre())
                .apellidos(user.getApellidos())
                .nombreCompleto(user.getNombreCompleto())
                .avatarUrl(user.getAvatarUrl())
                .puestoTrabajo(user.getPuestoTrabajo())
                .empresaId(user.getEmpresaId())
                .rolId(rol.getRolId())
                .codigoRol(rol.getCodigoRol())
                .nombreRol(rol.getNombreRol())
                .activo(user.isActivo())
                .terminosAceptados(user.isTerminosAceptados())
                .fechaRegistro(user.getFechaRegistro())
                .ultimoLogin(user.getUltimoLogin())
                .actualizadoEn(user.getActualizadoEn())
                .build();
    }
}
