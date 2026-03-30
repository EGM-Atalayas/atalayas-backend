package com.atalayas.backend.communication.mapper;

import com.atalayas.backend.communication.dto.AnnouncementRequest;
import com.atalayas.backend.communication.dto.AnnouncementResponse;
import com.atalayas.backend.communication.entity.Announcement;
import com.atalayas.backend.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class AnnouncementMapper {

    /**
     * Construye una entidad Announcement a partir del request y el usuario autenticado.
     *
     * @param request   payload validado
     * @param user      usuario autenticado (fuente de empresaId y creadoPor)
     * @param esGlobal  valor ya resuelto por el service según el rol del usuario
     */
    public Announcement toEntity(AnnouncementRequest request, User user, boolean esGlobal) {
        return Announcement.builder()
                .titulo(request.getTitulo())
                .contenido(request.getContenido())
                .esGlobal(esGlobal)
                // Si es global no se asocia a ninguna empresa
                .empresaId(esGlobal ? null : user.getEmpresaId())
                .creadoPor(user.getUsuarioId())
                .activo(true)
                .build();
    }

    public AnnouncementResponse toResponse(Announcement announcement) {
        return AnnouncementResponse.builder()
                .anuncioId(announcement.getAnuncioId())
                .empresaId(announcement.getEmpresaId())
                .titulo(announcement.getTitulo())
                .contenido(announcement.getContenido())
                .esGlobal(announcement.isEsGlobal())
                .activo(announcement.isActivo())
                .creadoPor(announcement.getCreadoPor())
                .creadoEn(announcement.getCreadoEn())
                .actualizadoEn(announcement.getActualizadoEn())
                .build();
    }
}

