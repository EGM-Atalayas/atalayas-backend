package com.atalayas.backend.content.mapper;

import com.atalayas.backend.content.dto.ContentRequest;
import com.atalayas.backend.content.dto.ContentResponse;
import com.atalayas.backend.content.dto.QuestionResponse;
import com.atalayas.backend.content.entity.ContentItem;
import com.atalayas.backend.content.entity.Question;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * Mapper para convertir entre ContentItem / Question y sus DTOs
 * Incluye también la conversión de preguntas porque siempre
 * viajan embebidas dentro del ContentResponse
 */
@Component
public class ContentMapper {


    /**
     * Construye un ContentItem listo para persistir
     * La empresaId ya viene resuelta desde el service según el rol del usuario
     *
     * @param request   payload validado del controller
     * @param empresaId empresa ya resuelta según el rol del usuario
     */
    public ContentItem toEntity(ContentRequest request, UUID empresaId) {
        return ContentItem.builder()
                .moduloId(request.getModuloId())
                .empresaId(empresaId)
                .titulo(request.getTitulo())
                .descripcion(request.getDescripcion())
                .tipoContenido(request.getTipoContenido())
                .urlRecurso(request.getUrlRecurso())
                .cuerpoTexto(request.getCuerpoTexto())
                .orden(request.getOrden())
                .version(request.getVersion())
                .minutosEstimados(request.getMinutosEstimados())
                .esIaGenerado(request.isEsIaGenerado())
                .activo(request.isActivo())
                .build();
    }


    /**
     * Convierte un ContentItem a su DTO de respuesta
     * Incluye la lista de preguntas ya mapeadas - puede ser lista vacía
     * si el contenido no es de tipo EVALUACION
     */
    public ContentResponse toResponse(ContentItem c, List<Question> preguntas) {
        return ContentResponse.builder()
                .contenidoId(c.getContenidoId())
                .moduloId(c.getModuloId())
                .empresaId(c.getEmpresaId())
                .titulo(c.getTitulo())
                .descripcion(c.getDescripcion())
                .tipoContenido(c.getTipoContenido())
                .urlRecurso(c.getUrlRecurso())
                .cuerpoTexto(c.getCuerpoTexto())
                .orden(c.getOrden())
                .version(c.getVersion())
                .minutosEstimados(c.getMinutosEstimados())
                .esIaGenerado(c.isEsIaGenerado())
                .activo(c.isActivo())
                .preguntas(preguntas.stream().map(this::toQuestionResponse).collect(Collectors.toList()))
                .fechaCreacion(c.getFechaCreacion())
                .actualizadoEn(c.getActualizadoEn())
                .build();
    }


    /**
     * Convierte una pregunta de evaluación a su DTO de respuesta
     * Método separado porque QuestionController también puede necesitarlo en el futuro
     */
    public QuestionResponse toQuestionResponse(Question q) {
        return QuestionResponse.builder()
                .preguntaId(q.getPreguntaId())
                .contenidoId(q.getContenidoId())
                .enunciado(q.getEnunciado())
                .respuestaCorrecta(q.getRespuestaCorrecta())
                .actualizadoEn(q.getActualizadoEn())
                .build();
    }
}