package com.atalayas.backend.communication.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;


/**
 * Tabla: notificacion
 *
 * Notificaciones individuales dirigidas a un usuario concreto
 * Se generan automáticamente por eventos del sistema
 * (nuevo módulo publicado, progreso completado, anuncio nuevo, etc.)
 * o manualmente por un ROLE_ADMIN / ROLE_ADMIN_EMPRESA
 *
 * Visibilidad: cada usuario solo ve sus propias notificaciones
 * Marcado de lectura: el propio destinatario puede marcarlas como leídas
 */
@Entity
@Table(name = "notificacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "notificacion_id", updatable = false, nullable = false)
    private UUID notificacionId;

    /** ID del usuario destinatario de la notificación */
    @Column(name = "destinatario_id", nullable = false)
    private UUID destinatarioId;


    /**
     * Tipo de notificación para que el frontend pueda renderizar
     * el icono y color adecuado
     * Ejemplos: "MODULO_NUEVO", "ANUNCIO", "PROGRESO", "BIENVENIDA", "COMUNICADO"
     */
    @Column(name = "tipo", nullable = false, length = 100)
    private String tipo;

    @Column(name = "mensaje", nullable = false, columnDefinition = "TEXT")
    private String mensaje;


    /**
     * Enlace opcional al recurso relacionado
     * El frontend lo usa para redirigir al hacer clic en la notificación
     */
    @Column(name = "enlace", length = 500)
    private String enlace;


    /**
     * Estado de lectura
     * false = no leída (aparece como pendiente en la campana)
     * true  = leída (pasa al historial)
     */
    @Column(name = "leido", nullable = false)
    @Builder.Default
    private boolean leido = false;

    /** Gestionado por trigger en BD - no asignar manualmente en updates */
    @Column(name = "creado_en")
    private OffsetDateTime creadoEn;

    /** Gestionado por trigger en BD - no asignar manualmente en updates */
    @Column(name = "actualizado_en")
    private OffsetDateTime actualizadoEn;

    /** Fija creado_en a now() si no viene informado */
    @PrePersist
    protected void onCreate() {
        if (creadoEn == null) {
            creadoEn = OffsetDateTime.now();
        }
    }
}