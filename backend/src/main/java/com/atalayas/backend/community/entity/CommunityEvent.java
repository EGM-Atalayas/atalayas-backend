package com.atalayas.backend.community.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tabla: evento_comunidad
 *
 * <p>🔀 Mixto: filtrado por empresaId OR global.</p>
 * <ul>
 *   <li>Si {@code esGlobal = true}: visible para todos los usuarios de la plataforma.</li>
 *   <li>Si {@code esGlobal = false}: visible solo para usuarios cuyo empresaId coincida.</li>
 * </ul>
 *
 * <p>Query de listado: {@code WHERE (empresa_id = :empresaId OR es_global = true) AND activo = true}</p>
 * <p>Solo SUPER_ADMIN o ADMIN pueden crear eventos con {@code esGlobal = true}.</p>
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

    /**
     * Empresa propietaria del evento.
     * Nullable cuando {@code esGlobal = true}.
     */
    @Column(name = "empresa_id")
    private UUID empresaId;

    @Column(name = "titulo", nullable = false, length = 255)
    private String titulo;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    /**
     * Indica si el evento es visible para toda la plataforma.
     * Cuando es {@code true}, {@code empresaId} puede ser null.
     */
    @Column(name = "es_global", nullable = false)
    @Builder.Default
    private boolean esGlobal = false;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    /** ID del usuario que creó el evento. */
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

