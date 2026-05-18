package com.atalayas.backend.community.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad mapeada a la tabla 'evento_comunidad'
 *
 * Un evento puede ser global (visible para toda la plataforma)
 * o específico de una empresa. Los eventos globales los crea EGM,
 * los de empresa los crea el admin empresa para sus empleados
 */
@Entity
@Table(name = "evento_comunidad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "evento_id", updatable = false, nullable = false)
    private UUID eventoId;

    // Empresa propietaria del evento
    @Column(name = "empresa_id")
    private UUID empresaId;

    @Column(name = "titulo", nullable = false, length = 255)
    private String titulo;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    // true = visible para toda la plataforma
    // false = solo para la empresa
    @Column(name = "es_global", nullable = false)
    @Builder.Default
    private boolean esGlobal = false;

    @Column(name = "fecha_inicio", nullable = false)
    private OffsetDateTime fechaInicio;

    // Fecha de fin opcional
    @Column(name = "fecha_fin")
    private OffsetDateTime fechaFin;

    // ── Ubicación (todos opcionales) ────────────────────────────
    // Dirección o nombre del lugar en texto libre
    @Column(name = "lugar", length = 255)
    private String lugar;

    // Coordenadas para mostrar en mapa
    @Column(name = "latitud", precision = 10, scale = 7)
    private BigDecimal latitud;

    @Column(name = "longitud", precision = 10, scale = 7)
    private BigDecimal longitud;

    // URL pública de la imagen de portada (subida a Supabase Storage)
    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    // Soft delete
    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    // ID del usuario que creó el evento
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