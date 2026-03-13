package com.atalayas.backend.communication.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tabla: anuncio
 *
 * <p>🔀 Mixto: filtrado por empresaId OR global.</p>
 * <ul>
 *   <li>Si {@code esGlobal = true}: visible para todos los usuarios de la plataforma.</li>
 *   <li>Si {@code esGlobal = false}: visible solo para usuarios cuyo empresaId coincida.</li>
 * </ul>
 *
 * <p>Query de listado: {@code WHERE (empresa_id = :empresaId OR es_global = true) AND activo = true}</p>
 * <p>Solo SUPER_ADMIN o ADMIN pueden crear anuncios con {@code esGlobal = true}.</p>
 */
@Entity
@Table(name = "anuncio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "anuncio_id", updatable = false, nullable = false)
    private UUID anuncioId;

    /**
     * Empresa propietaria del anuncio.
     * Nullable cuando {@code esGlobal = true}.
     */
    @Column(name = "empresa_id")
    private UUID empresaId;

    @Column(name = "titulo", nullable = false, length = 255)
    private String titulo;

    @Column(name = "contenido", columnDefinition = "TEXT")
    private String contenido;

    /**
     * Indica si el anuncio es visible para toda la plataforma.
     * Cuando es {@code true}, {@code empresaId} puede ser null.
     */
    @Column(name = "es_global", nullable = false)
    @Builder.Default
    private boolean esGlobal = false;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    /** ID del usuario que creó el anuncio. */
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

