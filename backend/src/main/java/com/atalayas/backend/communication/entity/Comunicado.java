package com.atalayas.backend.communication.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;


/**
 * Tabla: comunicado
 *
 * Comunicados oficiales de EGM Atalayas Ciudad Empresarial dirigidos
 * a todos los usuarios de la plataforma
 *
 * Solo ROLE_ADMIN puede crear y desactivar comunicados
 * Todos los usuarios autenticados los ven
 *
 * Visibilidad: activo = true AND (fecha_expiracion IS NULL OR fecha_expiracion > now())
 */
@Entity
@Table(name = "comunicado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comunicado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "comunicado_id", updatable = false, nullable = false)
    private UUID comunicadoId;

    /** ID del usuario ROLE_ADMIN que creó el comunicado. */
    @Column(name = "creado_por")
    private UUID creadoPor;

    @Column(name = "titulo", nullable = false, length = 250)
    private String titulo;

    @Column(name = "mensaje", nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    /** URL opcional de imagen adjunta o decorativa. */
    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    /**
     * Fecha de publicación. Si no se envía en el request,
     * el @PrePersist la fija a now() automáticamente
     * Permite programar comunicados futuros enviando una fecha posterior
     */
    @Column(name = "fecha_publicacion")
    private OffsetDateTime fechaPublicacion;

    /**
     * Fecha de expiración opcional.
     * Null = el comunicado no caduca automáticamente
     * Si es pasada, se filtra en las queries de listado
     */
    @Column(name = "fecha_expiracion")
    private OffsetDateTime fechaExpiracion;

    /**
     * Control manual de visibilidad (soft-delete)
     * Permite retirar un comunicado de forma inmediata
     * independientemente de las fechas
     */
    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    /** Gestionado por trigger en BD — no asignar manualmente en updates */
    @Column(name = "actualizado_en")
    private OffsetDateTime actualizadoEn;

    /** Fija fecha_publicacion a now() si no viene informada en el request */
    @PrePersist
    protected void onCreate() {
        if (fechaPublicacion == null) {
            fechaPublicacion = OffsetDateTime.now();
        }
    }
}