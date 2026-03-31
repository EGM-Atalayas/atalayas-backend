package com.atalayas.backend.communication.mapper;

import com.atalayas.backend.communication.dto.NotificacionRequest;
import com.atalayas.backend.communication.dto.NotificacionResponse;
import com.atalayas.backend.communication.entity.Notificacion;
import org.springframework.stereotype.Component;

@Component
public class NotificacionMapper {

    /**
     * Construye la entidad a partir del request
     * leido arranca siempre en false - nunca se acepta del body
     */
    public Notificacion toEntity(NotificacionRequest request) {
        return Notificacion.builder()
                .destinatarioId(request.getDestinatarioId())
                .tipo(request.getTipo())
                .mensaje(request.getMensaje())
                .enlace(request.getEnlace())
                .leido(false)
                .build();
    }

    public NotificacionResponse toResponse(Notificacion notificacion) {
        return NotificacionResponse.builder()
                .notificacionId(notificacion.getNotificacionId())
                .destinatarioId(notificacion.getDestinatarioId())
                .tipo(notificacion.getTipo())
                .mensaje(notificacion.getMensaje())
                .enlace(notificacion.getEnlace())
                .leido(notificacion.isLeido())
                .creadoEn(notificacion.getCreadoEn())
                .actualizadoEn(notificacion.getActualizadoEn())
                .build();
    }
}