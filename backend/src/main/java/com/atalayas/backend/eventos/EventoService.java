package com.atalayas.backend.eventos;

import com.atalayas.backend.eventos.dto.EventoRequest;
import com.atalayas.backend.eventos.dto.EventoResponse;
import com.atalayas.backend.eventos.entity.Evento;
import com.atalayas.backend.eventos.enums.EstadoEvento;
import com.atalayas.backend.eventos.mapper.EventoMapper;
import com.atalayas.backend.eventos.repository.EventoRepository;
import com.atalayas.backend.exception.ResourceNotFoundException;
import com.atalayas.backend.usuario.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventoService {

    private final EventoRepository eventoRepository;
    private final EventoMapper eventoMapper;

    @Transactional
    public EventoResponse crear(EventoRequest request, User user) {
        Evento guardado = eventoRepository.save(
                eventoMapper.toEntity(request, user.getUsuarioId()));
        log.info("Evento creado: {} por usuario: {}", guardado.getEventoId(), user.getEmail());
        return eventoMapper.toResponse(guardado);
    }

    /** Lista todos los eventos activos para cualquier usuario autenticado */
    public List<EventoResponse> listar() {
        return eventoRepository.findActivosOrdenados()
                .stream().map(eventoMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public EventoResponse actualizar(UUID eventoId, EventoRequest request, User user) {
        Evento evento = findOrThrow(eventoId);

        evento.setTitulo(request.getTitulo());
        evento.setDescripcion(request.getDescripcion());
        evento.setFecha(request.getFecha());
        evento.setHoraInicio(request.getHoraInicio());
        evento.setHoraFin(request.getHoraFin());
        evento.setLugar(request.getLugar());
        evento.setUrlInfo(request.getUrlInfo());
        evento.setImagenUrl(request.getImagenUrl());

        log.info("Evento actualizado: {} por usuario: {}", eventoId, user.getEmail());
        return eventoMapper.toResponse(eventoRepository.save(evento));
    }

    @Transactional
    public EventoResponse cancelar(UUID eventoId, User user) {
        Evento evento = findOrThrow(eventoId);

        if (evento.getEstado() == EstadoEvento.CANCELADO) {
            throw new IllegalStateException("El evento ya está cancelado");
        }

        evento.setEstado(EstadoEvento.CANCELADO);
        evento.setActivo(false);
        log.info("Evento cancelado: {} por usuario: {}", eventoId, user.getEmail());
        return eventoMapper.toResponse(eventoRepository.save(evento));
    }

    private Evento findOrThrow(UUID eventoId) {
        return eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado: " + eventoId));
    }
}
