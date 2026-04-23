package com.atalayas.backend.communication.mapper;

import com.atalayas.backend.communication.dto.NotificationRequest;
import com.atalayas.backend.communication.dto.NotificationResponse;
import com.atalayas.backend.communication.entity.Notification;
import org.springframework.stereotype.Component;

/**
 * Mapper para convertir entre la entidad Notification y sus DTOs
 */
@Component
public class NotificationMapper {

    /**
     * Construye la entidad a partir del request.
     */
    public Notification toEntity(NotificationRequest request) {
        return Notification.builder()
                .destinatarioId(request.getDestinatarioId())
                .tipo(request.getTipo())
                .mensaje(request.getMensaje())
                .enlace(request.getEnlace())
                .leido(false)
                .build();
    }

    /**
     * Convierte la entidad en su DTO de respuesta.
     */
    public NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .notificacionId(notification.getNotificacionId())
                .destinatarioId(notification.getDestinatarioId())
                .tipo(notification.getTipo())
                .mensaje(notification.getMensaje())
                .enlace(notification.getEnlace())
                .leido(notification.isLeido())
                .creadoEn(notification.getCreadoEn())
                .actualizadoEn(notification.getActualizadoEn())
                .build();
    }
}