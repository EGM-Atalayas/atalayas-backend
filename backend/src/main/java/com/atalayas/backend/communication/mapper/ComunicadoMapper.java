package com.atalayas.backend.communication.mapper;

import com.atalayas.backend.communication.dto.ComunicadoRequest;
import com.atalayas.backend.communication.dto.ComunicadoResponse;
import com.atalayas.backend.communication.entity.Comunicado;
import com.atalayas.backend.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ComunicadoMapper {

    /**
     * Construye la entidad Comunicado a partir del request y el usuario autenticado
     * creadoPor se toma siempre del usuario autenticado, nunca del body
     *
     * @param request payload validado
     * @param user    usuario ROLE_ADMIN autenticado
     */
    public Comunicado toEntity(ComunicadoRequest request, User user) {
        return Comunicado.builder()
                .titulo(request.getTitulo())
                .mensaje(request.getMensaje())
                .imagenUrl(request.getImagenUrl())
                .fechaPublicacion(request.getFechaPublicacion())
                .fechaExpiracion(request.getFechaExpiracion())
                .creadoPor(user.getUsuarioId())
                .activo(true)
                .build();
    }

    public ComunicadoResponse toResponse(Comunicado comunicado) {
        return ComunicadoResponse.builder()
                .comunicadoId(comunicado.getComunicadoId())
                .creadoPor(comunicado.getCreadoPor())
                .titulo(comunicado.getTitulo())
                .mensaje(comunicado.getMensaje())
                .imagenUrl(comunicado.getImagenUrl())
                .fechaPublicacion(comunicado.getFechaPublicacion())
                .fechaExpiracion(comunicado.getFechaExpiracion())
                .activo(comunicado.isActivo())
                .actualizadoEn(comunicado.getActualizadoEn())
                .build();
    }
}