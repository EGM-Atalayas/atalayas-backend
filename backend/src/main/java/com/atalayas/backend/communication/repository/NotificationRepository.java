package com.atalayas.backend.communication.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.atalayas.backend.communication.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // Todas las notificaciones de un usuario, más recientes primero
    List<Notification> findByDestinatarioIdOrderByCreadoEnDesc(UUID destinatarioId);

    // Paginado — para el panel con scroll
    Page<Notification> findByDestinatarioIdOrderByCreadoEnDesc(UUID destinatarioId, Pageable pageable);

    // Solo las no leídas
    List<Notification> findByDestinatarioIdAndLeidoFalseOrderByCreadoEnDesc(UUID destinatarioId);

    // Contador de no leídas
    long countByDestinatarioIdAndLeidoFalse(UUID destinatarioId);

    @Modifying
    @Query("UPDATE Notification n SET n.leido = true WHERE n.destinatarioId = :destinatarioId AND n.leido = false")
    int marcarTodasComoLeidas(@Param("destinatarioId") UUID destinatarioId);
}