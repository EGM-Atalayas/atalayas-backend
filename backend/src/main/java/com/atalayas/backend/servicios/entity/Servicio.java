package com.atalayas.backend.servicios.entity;

import com.atalayas.backend.servicios.enums.CategoriaServicio;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Servicios que EGM Atalayas ofrece directamente a los trabajadores del parque.
 * Son siempre globales (no pertenecen a ninguna empresa en concreto).
 *
 * Ejemplos: autobús lanzadera, ludoteca, coche compartido, empresas solidarias...
 */
@Entity
@Table(name = "servicio")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "servicio_id", updatable = false, nullable = false)
    private UUID servicioId;

    @Column(name = "titulo", nullable = false, length = 255)
    private String titulo;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false, length = 50)
    private CategoriaServicio categoria;

    @Column(name = "icono_url", length = 500)
    private String iconoUrl;

    @Column(name = "url_info", length = 500)
    private String urlInfo;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "como_acceder", columnDefinition = "TEXT")
    private String comoAcceder;

    @Column(name = "creado_por")
    private UUID creadoPor;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

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
