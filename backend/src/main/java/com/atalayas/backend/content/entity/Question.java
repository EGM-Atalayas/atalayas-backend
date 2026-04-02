package com.atalayas.backend.content.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;


/**
 * Entidad que representa una pregunta de evaluación en la tabla 'contenido_pregunta'
 * Solo aplica a contenidos de tipo EVALUACION
 * Las respuestas del empleado se guardan en 'respuesta_empleado'
 */
@Entity
@Table(name = "contenido_pregunta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "pregunta_id", updatable = false, nullable = false)
    private UUID preguntaId;

    // FK al contenido de tipo EVALUACION al que pertenece esta pregunta
    @Column(name = "contenido_id", nullable = false)
    private UUID contenidoId;

    // Texto de la pregunta que verá el empleado
    @Column(name = "enunciado", nullable = false, columnDefinition = "TEXT")
    private String enunciado;

    // Respuesta correcta - null en preguntas abiertas sin corrección automática
    @Column(name = "respuesta_correcta", columnDefinition = "TEXT")
    private String respuestaCorrecta;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;


    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = LocalDateTime.now();
    }
}