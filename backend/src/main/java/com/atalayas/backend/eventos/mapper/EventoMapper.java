package com.atalayas.backend.eventos.mapper;

import com.atalayas.backend.eventos.dto.EventoRequest;
import com.atalayas.backend.eventos.dto.EventoResponse;
import com.atalayas.backend.eventos.entity.Evento;
import com.atalayas.backend.eventos.enums.EstadoEvento;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class EventoMapper {

    public Evento toEntity(EventoRequest request, UUID creadoPor) {
        return Evento.builder()
                .titulo(request.getTitulo())
                .descripcion(request.getDescripcion())
                .fecha(request.getFecha())
                .horaInicio(request.getHoraInicio())
                .horaFin(request.getHoraFin())
                .lugar(request.getLugar())
                .urlInfo(request.getUrlInfo())
                .imagenUrl(request.getImagenUrl())
                .estado(EstadoEvento.PROXIMO)
                .creadoPor(creadoPor)
                .activo(true)
                .build();
    }

    public EventoResponse toResponse(Evento e) {
        return EventoResponse.builder()
                .eventoId(e.getEventoId())
                .titulo(e.getTitulo())
                .descripcion(e.getDescripcion())
                .fecha(e.getFecha())
                .horaInicio(e.getHoraInicio())
                .horaFin(e.getHoraFin())
                .lugar(e.getLugar())
                .urlInfo(e.getUrlInfo())
                .imagenUrl(e.getImagenUrl())
                .estado(e.getEstado())
                .creadoPor(e.getCreadoPor())
                .activo(e.isActivo())
                .creadoEn(e.getCreadoEn())
                .actualizadoEn(e.getActualizadoEn())
                .build();
    }
}
