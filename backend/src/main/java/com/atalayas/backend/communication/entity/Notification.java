package com.atalayas.backend.communication.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad mapeada a la tabla 'notificacion'
 *
 * Las notificaciones son mensajes individuales dirigidos a un usuario concreto.
 * Se generan automáticamente por eventos del sistema, módulo nuevo publicado,
 * contenido completado, bienvenida al registrarse, etc. O manualmente
 * por un admin que quiere avisar a un empleado concreto
 *
 * Cada usuario solo puede ver sus propias notificaciones
 * El propio destinatario es quien las marca como leídas
 */
@Entity
@Table(name = "notificacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "notificacion_id", updatable = false, nullable = false)
    private UUID notificacionId;

    // ID del usuario que recibirá esta notificación
    @Column(name = "destinatario_id", nullable = false)
    private UUID destinatarioId;

    /**
     * Tipo de notificación, el frontend lo usa para elegir el icono y color.
     * Valores posibles: MODULO_NUEVO, CONTENIDO_COMPLETADO, ANUNCIO,
     * BIENVENIDA, COMUNICADO, CONTENIDO_ACTUALIZADO
     */
    @Column(name = "tipo", nullable = false, length = 100)
    private String tipo;

    @Column(name = "mensaje", nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    // Enlace opcional al recurso relacionado
    @Column(name = "enlace", length = 500)
    private String enlace;

    /**
     * Estado de lectura.
     * false = no leída, aparece como pendiente en la campana del header.
     * true  = leída, pasa al historial sin badge de alerta.
     * Una vez marcada como leída no puede volver a false.
     */
    @Column(name = "leido", nullable = false)
    @Builder.Default
    private boolean leido = false;

    // Gestionado por trigger en BD
    @Column(name = "creado_en")
    private OffsetDateTime creadoEn;

    // Gestionado por trigger en BD
    @Column(name = "actualizado_en")
    private OffsetDateTime actualizadoEn;

    @PrePersist
    protected void onCreate() {
        if (creadoEn == null) {
            creadoEn = OffsetDateTime.now();
        }
    }
}