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
                .enlaceUrl(request.getEnlaceUrl())
                .enlaceTexto(request.getEnlaceTexto())
                .videoUrl(request.getVideoUrl())
                .adjuntoUrl(request.getAdjuntoUrl())
                .adjuntoNombre(request.getAdjuntoNombre())
                .estado(request.getEstado() != null ? request.getEstado() : "publicado")
                .fijado(request.isFijado())
                .categoria(request.getCategoria())
                .esGlobal(esGlobal)
                .empresaId(esGlobal ? null : user.getEmpresaId())
                .creadoPor(user.getUsuarioId())
                .activo(true)
                .build();
    }

    /**
     * Aplica los campos editables de un AnnouncementRequest sobre una entidad existente.
     * No modifica: anuncioId, empresaId, creadoPor, creadoEn, activo, esGlobal (seguridad).
     */
    public void updateEntity(Announcement announcement, AnnouncementRequest request) {
        announcement.setTitulo(request.getTitulo());
        announcement.setContenido(request.getContenido());
        announcement.setImagenUrl(request.getImagenUrl());
        announcement.setEnlaceUrl(request.getEnlaceUrl());
        announcement.setEnlaceTexto(request.getEnlaceTexto());
        announcement.setVideoUrl(request.getVideoUrl());
        announcement.setAdjuntoUrl(request.getAdjuntoUrl());
        announcement.setAdjuntoNombre(request.getAdjuntoNombre());
        if (request.getEstado() != null) {
            announcement.setEstado(request.getEstado());
        }
        announcement.setFijado(request.isFijado());
        announcement.setCategoria(request.getCategoria());
    }

    public AnnouncementResponse toResponse(Announcement announcement) {
        return AnnouncementResponse.builder()
                .anuncioId(announcement.getAnuncioId())
                .empresaId(announcement.getEmpresaId())
                .titulo(announcement.getTitulo())
                .contenido(announcement.getContenido())
                .imagenUrl(announcement.getImagenUrl())
                .enlaceUrl(announcement.getEnlaceUrl())
                .enlaceTexto(announcement.getEnlaceTexto())
                .videoUrl(announcement.getVideoUrl())
                .adjuntoUrl(announcement.getAdjuntoUrl())
                .adjuntoNombre(announcement.getAdjuntoNombre())
                .estado(announcement.getEstado())
                .fijado(announcement.isFijado())
                .vistas(announcement.getVistas())
                .categoria(announcement.getCategoria())
                .esGlobal(announcement.isEsGlobal())
                .activo(announcement.isActivo())
                .creadoPor(announcement.getCreadoPor())
                .creadoEn(announcement.getCreadoEn())
                .actualizadoEn(announcement.getActualizadoEn())
                .build();
    }
}
