package com.atalayas.backend.communication.mapper;

import com.atalayas.backend.communication.dto.AnnouncementRequest;
import com.atalayas.backend.communication.dto.AnnouncementResponse;
import com.atalayas.backend.communication.entity.Announcement;
import com.atalayas.backend.user.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper para convertir entre la entidad Announcement y sus DTOs
 */
@Component
public class AnnouncementMapper {

    /**
     * Construye una entidad Announcement a partir del request y el usuario autenticado.
     *
     * El flag esGlobal ya viene resuelto desde el servicio según el rol del usuario,
     * aquí solo lo asignamos, no lo calculamos.
     *
     * Si el anuncio es global no se asocia a ninguna empresa (empresaId = null).
     * Si no es global, heredamos el empresaId del usuario que lo crea.
     */
    public Announcement toEntity(AnnouncementRequest request, User user, boolean esGlobal) {
        return Announcement.builder()
                .titulo(request.getTitulo())
                .contenido(request.getContenido())
                .esGlobal(esGlobal)
                .empresaId(esGlobal ? null : user.getEmpresaId())
                .creadoPor(user.getUsuarioId())
                .activo(true)
                .build();
    }

    /**
     * Convierte una entidad Announcement en su DTO de respuesta.
     * Expone todos los campos que el frontend necesita para mostrar el anuncio.
     */
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