package com.atalayas.backend.module.mapper;

import com.atalayas.backend.module.dto.ModuleRequest;
import com.atalayas.backend.module.dto.ModuleResponse;
import com.atalayas.backend.module.entity.TrainingModule;
import com.atalayas.backend.usuario.entity.User;
import org.springframework.stereotype.Component;


/**
 * Mapper para convertir entre la entidad TrainingModule y sus DTOs
 * Centraliza la lógica de conversión para que los services no tengan
 * métodos toResponse() privados dispersos — mismo patrón que AnnouncementMapper
 */
@Component
public class ModuleMapper {


    /**
     * Construye una entidad TrainingModule lista para persistir
     * Si el usuario es admin empresa, su empresaId sobreescribe siempre
     * el del request — esto se resuelve en el service antes de llamar aquí
     *
     * @param request   payload validado del controller
     * @param empresaId empresa ya resuelta según el rol del usuario
     */
    public TrainingModule toEntity(ModuleRequest request, java.util.UUID empresaId) {
        return TrainingModule.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .empresaId(empresaId)
                .tipoModulo(request.getTipoModulo())
                .orden(request.getOrden())
                .esEspecializadoIa(request.isEsEspecializadoIa())
                .activo(request.isActivo())
                .idioma(request.getIdioma() != null ? request.getIdioma() : "es")
                .duracion(request.getDuracion())
                .audiencia(request.getAudiencia() != null ? request.getAudiencia() : "todos")
                .departamentos(request.getDepartamentos())
                .testPreguntas(request.getTestPreguntas())
                .imagenPortadaUrl(request.getImagenPortadaUrl())
                .tiposSalida(request.getTiposSalida() != null ? request.getTiposSalida() : "documentacion")
                .contenidoMarkdown(request.getContenidoMarkdown())
                .scriptPodcast(request.getScriptPodcast())
                .scriptVideo(request.getScriptVideo())
                .podcastAudioUrl(request.getPodcastAudioUrl())
                .adjuntoUrl(request.getAdjuntoUrl())
                .adjuntoNombre(request.getAdjuntoNombre())
                .build();
    }


    /**
     * Convierte una entidad a su DTO de respuesta
     * El nombreEmpresa se deja null intencionalmente en esta fase del MVP -
     * se enriquecerá cuando se integre CompanyService si el frontend lo necesita
     */
    public ModuleResponse toResponse(TrainingModule m) {
        return ModuleResponse.builder()
                .moduloId(m.getModuloId())
                .nombre(m.getNombre())
                .descripcion(m.getDescripcion())
                .empresaId(m.getEmpresaId())
                .tipoModulo(m.getTipoModulo())
                .orden(m.getOrden())
                .esEspecializadoIa(m.isEsEspecializadoIa())
                .activo(m.isActivo())
                .idioma(m.getIdioma())
                .duracion(m.getDuracion())
                .audiencia(m.getAudiencia())
                .departamentos(m.getDepartamentos())
                .testPreguntas(m.getTestPreguntas())
                .imagenPortadaUrl(m.getImagenPortadaUrl())
                .tiposSalida(m.getTiposSalida())
                .contenidoMarkdown(m.getContenidoMarkdown())
                .scriptPodcast(m.getScriptPodcast())
                .scriptVideo(m.getScriptVideo())
                .podcastAudioUrl(m.getPodcastAudioUrl())
                .adjuntoUrl(m.getAdjuntoUrl())
                .adjuntoNombre(m.getAdjuntoNombre())
                .fechaCreacion(m.getFechaCreacion())
                .actualizadoEn(m.getActualizadoEn())
                .build();
    }
}