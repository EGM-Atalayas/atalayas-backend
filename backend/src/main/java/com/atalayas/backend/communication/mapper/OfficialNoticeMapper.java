package com.atalayas.backend.communication.mapper;

import com.atalayas.backend.communication.dto.OfficialNoticeRequest;
import com.atalayas.backend.communication.dto.OfficialNoticeResponse;
import com.atalayas.backend.communication.entity.OfficialNotice;
import com.atalayas.backend.user.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper para convertir entre la entidad OfficialNotice y sus DTOs
 *
 * El campo creadoPor se toma siempre del usuario autenticado,
 * nunca del body de la petición, evita suplantaciones
 */
@Component
public class OfficialNoticeMapper {

    /**
     * Construye la entidad a partir del request y el superadmin autenticado,
     * activo arranca siempre en true, se desactiva manualmente después si hace falta.
     */
    public OfficialNotice toEntity(OfficialNoticeRequest request, User user) {
        return OfficialNotice.builder()
                .titulo(request.getTitulo())
                .mensaje(request.getMensaje())
                .imagenUrl(request.getImagenUrl())
                .categoria(request.getCategoria())
                .destacado(request.getDestacado() != null ? request.getDestacado() : false)
                .estado(request.getEstado() != null ? request.getEstado() : "publicado")
                .enlaceUrl(request.getEnlaceUrl())
                .enlaceTexto(request.getEnlaceTexto())
                .videoUrl(request.getVideoUrl())
                .adjuntoUrl(request.getAdjuntoUrl())
                .adjuntoNombre(request.getAdjuntoNombre())
                .fechaPublicacion(request.getFechaPublicacion())
                .fechaExpiracion(request.getFechaExpiracion())
                .creadoPor(user.getUsuarioId())
                .activo(true)
                .build();
    }

    /**
     * Convierte la entidad en su DTO de respuesta.
     * Expone todos los campos que el frontend necesita para mostrar el comunicado.
     */
    public OfficialNoticeResponse toResponse(OfficialNotice notice) {
        return OfficialNoticeResponse.builder()
                .comunicadoId(notice.getComunicadoId())
                .creadoPor(notice.getCreadoPor())
                .titulo(notice.getTitulo())
                .mensaje(notice.getMensaje())
                .imagenUrl(notice.getImagenUrl())
                .categoria(notice.getCategoria())
                .destacado(notice.isDestacado())
                .estado(notice.getEstado())
                .enlaceUrl(notice.getEnlaceUrl())
                .enlaceTexto(notice.getEnlaceTexto())
                .videoUrl(notice.getVideoUrl())
                .adjuntoUrl(notice.getAdjuntoUrl())
                .adjuntoNombre(notice.getAdjuntoNombre())
                .fechaPublicacion(notice.getFechaPublicacion())
                .fechaExpiracion(notice.getFechaExpiracion())
                .activo(notice.isActivo())
                .actualizadoEn(notice.getActualizadoEn())
                .build();
    }
}