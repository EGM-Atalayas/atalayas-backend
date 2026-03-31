package com.atalayas.backend.rewards.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;


/**
 * Entidad que representa un beneficio o ventaja del área empresarial
 * Mapeada a la tabla 'beneficio'
 *
 * Los beneficios pueden ser globales (visibles para todos) o
 * específicos de una empresa
 * Ejemplos: coche compartido, guardería, etc
 */
@Entity
@Table(name = "beneficio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Benefit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "beneficio_id", updatable = false, nullable = false)
    private UUID beneficioId;

    // null = beneficio global visible para todos los empleados del área
    @Column(name = "empresa_id")
    private UUID empresaId;

    // Título visible del beneficio en la plataforma
    @Column(name = "titulo", nullable = false, length = 255)
    private String titulo;

    // Descripción detallada del beneficio y cómo acceder a él
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    // URL opcional con más información o formulario de solicitud
    @Column(name = "url_info", length = 500)
    private String urlInfo;

    // Soft delete — conserva historial aunque se retire el beneficio
    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    // Usuario que creó el beneficio para trazabilidad
    @Column(name = "creado_por")
    private UUID creadoPor;

    @Column(name = "creado_en", updatable = false, nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @PrePersist
    protected void onCreate() {
        creadoEn = LocalDateTime.now();
        actualizadoEn = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = LocalDateTime.now();
    }
}