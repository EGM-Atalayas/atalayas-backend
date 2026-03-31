package com.atalayas.backend.communication.repository;

import com.atalayas.backend.communication.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, UUID> {


    /**
     * Todas las notificaciones de un usuario, más recientes primero
     * Usado por el propio usuario para ver su bandeja completa
     */
    List<Notificacion> findByDestinatarioIdOrderByCreadoEnDesc(UUID destinatarioId);

    /**
     * Solo las no leídas de un usuario (para la campana / badge)
     */
    List<Notificacion> findByDestinatarioIdAndLeidoFalseOrderByCreadoEnDesc(UUID destinatarioId);

    /**
     * Cuenta las no leídas - útil para el contador del frontend
     */
    long countByDestinatarioIdAndLeidoFalse(UUID destinatarioId);

    /**
     * Marca todas las notificaciones no leídas de un usuario como leídas de golpe
     * ROLE_ADMIN_EMPRESA y ROLE_EMPLEADO pueden usar esta acción sobre sus propias notifs
     */
    @Modifying
    @Query("UPDATE Notificacion n SET n.leido = true WHERE n.destinatarioId = :destinatarioId AND n.leido = false")
    int marcarTodasComoLeidas(@Param("destinatarioId") UUID destinatarioId);
}