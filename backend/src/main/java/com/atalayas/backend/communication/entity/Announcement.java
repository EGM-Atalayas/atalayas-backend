package com.atalayas.backend.communication.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad mapeada a la tabla 'anuncio'
 *
 * Los anuncios son comunicaciones de empresa a empleados. Pueden ser:
 *   - De empresa: visibles solo para los empleados de esa empresa (esGlobal = false)
 *   - Globales: visibles para todos los usuarios de la plataforma (esGlobal = true)
 *     Solo ROLE_ADMIN puede crear anuncios globales.
 *
 * Query de listado:
 *   WHERE (empresa_id = :empresaId OR es_global = true) AND activo = true
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

    // Empresa propietaria del anuncio, nullable cuando esGlobal = true
    @Column(name = "empresa_id")
    private UUID empresaId;

    @Column(name = "titulo", nullable = false, length = 255)
    private String titulo;

    @Column(name = "contenido", columnDefinition = "TEXT")
    private String contenido;

    // true = visible para todos los usuarios de la plataforma
    // false = visible solo para empleados de la empresa propietaria
    @Column(name = "es_global", nullable = false)
    @Builder.Default
    private boolean esGlobal = false;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    // ID del usuario que creó el anuncio
    @Column(name = "creado_por")
    private UUID creadoPor;

    @Column(name = "creado_en", updatable = false, nullable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Column(name = "enlace_url", columnDefinition = "TEXT")
    private String enlaceUrl;

    @Column(name = "enlace_texto", length = 100)
    private String enlaceTexto;

    @Column(name = "video_url", columnDefinition = "TEXT")
    private String videoUrl;

    @Column(name = "adjunto_url", columnDefinition = "TEXT")
    private String adjuntoUrl;

    @Column(name = "adjunto_nombre", length = 200)
    private String adjuntoNombre;

    /** 'publicado' | 'borrador' — default 'publicado' */
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "publicado";

    @Column(name = "fijado", nullable = false)
    @Builder.Default
    private boolean fijado = false;

    @Column(name = "vistas", nullable = false)
    @Builder.Default
    private int vistas = 0;

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