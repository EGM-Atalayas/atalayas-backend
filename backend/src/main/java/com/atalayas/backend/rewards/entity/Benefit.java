package com.atalayas.backend.rewards.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad mapeada a la tabla 'beneficio'
 *
 * Representa un beneficio o ventaja del área empresarial EGM
 * Puede ser global (visible para todos los empleados del parque)
 * o específico de una empresa concreta
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

    // Soft delete
    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    // ID del usuario que creó el beneficio
    @Column(name = "creado_por")
    private UUID creadoPor;

    // Gestionado automáticamente
    @Column(name = "creado_en", updatable = false, nullable = false)
    private OffsetDateTime creadoEn;

    // Gestionado automáticamente
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @PrePersist
    protected void onCreate() {
        creadoEn = OffsetDateTime.now();
        actualizadoEn = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = OffsetDateTime.now();
    }
}