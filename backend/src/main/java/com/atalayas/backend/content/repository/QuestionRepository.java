package com.atalayas.backend.content.repository;

import com.atalayas.backend.content.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;


/**
 * Repositorio JPA para la tabla 'contenido_pregunta'
 * Las preguntas siempre se consultan en bloque por contenido
 */
@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID> {

    // Todas las preguntas de un contenido de tipo EVALUACION
    List<Question> findByContenidoIdOrderByPreguntaId(UUID contenidoId);

    // Eliminar todas las preguntas de un contenido (al borrar el contenido)
    void deleteByContenidoId(UUID contenidoId);
}