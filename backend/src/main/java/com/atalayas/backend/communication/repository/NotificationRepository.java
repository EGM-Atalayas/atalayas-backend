package com.atalayas.backend.communication.repository;

import com.atalayas.backend.communication.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para la tabla 'notificacion'
 *
 * Las queries cubren tres casos de uso principales:
 *   1. Bandeja completa del usuario, todas sus notificaciones
 *   2. Campana del header, solo las no leídas para el badge contador
 *   3. Marcado masivo, marcar todas como leídas de una vez
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // Todas las notificaciones de un usuario, más recientes primero
    List<Notification> findByDestinatarioIdOrderByCreadoEnDesc(UUID destinatarioId);

    // Solo las no leídas
    List<Notification> findByDestinatarioIdAndLeidoFalseOrderByCreadoEnDesc(UUID destinatarioId);

    // Contador de no leídas
    long countByDestinatarioIdAndLeidoFalse(UUID destinatarioId);


    /**
     * Marca todas las notificaciones no leídas de un usuario como leídas de golpe.
     * Se usa cuando el usuario abre el panel de notificaciones en el header.
     * Devuelve cuántas filas se actualizaron para confirmación.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.leido = true WHERE n.destinatarioId = :destinatarioId AND n.leido = false")
    int marcarTodasComoLeidas(@Param("destinatarioId") UUID destinatarioId);
}