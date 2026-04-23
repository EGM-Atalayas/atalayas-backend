package com.atalayas.backend.communication.mapper;

import com.atalayas.backend.communication.dto.AnnouncementRequest;
import com.atalayas.backend.communication.dto.AnnouncementResponse;
import com.atalayas.backend.communication.entity.Announcement;
import com.atalayas.backend.user.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper para convertir entre la entidad Announcement y sus DTOs.
 */
@Component
public class AnnouncementMapper {

    public Announcement toEntity(AnnouncementRequest request, User user, boolean esGlobal) {
        return Announcement.builder()
                .titulo(request.getTitulo())
                .contenido(request.getContenido())
                .imagenUrl(request.getImagenUrl())
                .esGlobal(esGlobal)
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
                .imagenUrl(announcement.getImagenUrl())
                .esGlobal(announcement.isEsGlobal())
                .activo(announcement.isActivo())
                .creadoPor(announcement.getCreadoPor())
                .creadoEn(announcement.getCreadoEn())
                .actualizadoEn(announcement.getActualizadoEn())
                .build();
    }
}
