package com.atalayas.backend.content.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad mapeada a la tabla 'contenido_pregunta'
 *
 * Representa una pregunta de evaluación asociada a un contenido de tipo EVALUACION
 * Las respuestas del empleado se guardan en la tabla 'respuesta_empleado', no aquí
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

    // Texto de la pregunta que verá el empleado en el visor de contenido
    @Column(name = "enunciado", nullable = false, columnDefinition = "TEXT")
    private String enunciado;

    // Respuesta correcta
    @Column(name = "respuesta_correcta", columnDefinition = "TEXT")
    private String respuestaCorrecta;

    // Gestionado automáticamente
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = OffsetDateTime.now();
    }
}