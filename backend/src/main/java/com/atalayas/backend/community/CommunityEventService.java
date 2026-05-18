package com.atalayas.backend.community;

import com.atalayas.backend.common.enums.RoleType;
import com.atalayas.backend.community.dto.CommunityEventRequest;
import com.atalayas.backend.community.dto.CommunityEventResponse;
import com.atalayas.backend.community.entity.CommunityEvent;
import com.atalayas.backend.community.mapper.CommunityEventMapper;
import com.atalayas.backend.community.repository.CommunityEventRepository;
import com.atalayas.backend.exception.ResourceNotFoundException;
import com.atalayas.backend.exception.UnauthorizedException;
import com.atalayas.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * Lógica de negocio para eventos de comunidad
 *
 * Reglas de acceso:
 *   - ROLE_ADMIN         - acceso total, puede crear eventos globales
 *   - ROLE_ADMIN_EMPRESA - gestiona eventos de su empresa, no puede crear globales
 *   - ROLE_EMPLEADO      - solo lectura de eventos activos de su empresa + globales
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommunityEventService {

    private final CommunityEventRepository communityEventRepository;
    private final CommunityEventMapper communityEventMapper;


    // ── CREAR ────────────────────────────────────────────────────────────────
    /**
     * Crea un evento de comunidad
     * Admin empresa siempre crea en su empresa y no puede marcar esGlobal
     * Superadmin puede crear eventos globales visibles para toda la plataforma
     */
    @Transactional
    public CommunityEventResponse crear(CommunityEventRequest request, User user) {
        RoleType rol = user.getRol().getRoleType();

        // Admin empresa no puede crear eventos globales ni de otra empresa
        UUID empresaId = (rol == RoleType.ROLE_ADMIN_EMPRESA) ? user.getEmpresaId() : request.getEmpresaId();
        boolean esGlobal = (rol == RoleType.ROLE_ADMIN_EMPRESA) ? false : request.isEsGlobal();

        CommunityEvent guardado = communityEventRepository.save(
                communityEventMapper.toEntity(request, empresaId, esGlobal, user.getUsuarioId()));

        log.info("Evento creado: {} por usuario: {}", guardado.getEventoId(), user.getEmail());
        return communityEventMapper.toResponse(guardado);
    }


    // ── LISTAR ───────────────────────────────────────────────────────────────
    /**
     * Devuelve los eventos visibles según el rol:
     *   - Superadmin: todos los activos de la plataforma
     *   - Admin empresa: todos los de su empresa + globales activos
     *   - Empleado: activos de su empresa + globales activos (una sola query)
     */
    public List<CommunityEventResponse> listar(User user) {
        RoleType rol = user.getRol().getRoleType();

        if (rol == RoleType.ROLE_ADMIN) {
            return communityEventRepository.findByActivoTrueOrderByFechaInicioAsc()
                    .stream().map(communityEventMapper::toResponse).collect(Collectors.toList());
        }

        if (rol == RoleType.ROLE_ADMIN_EMPRESA) {
            // Admin ve todos los de su empresa (activos e inactivos) para gestionar
            List<CommunityEvent> todos = communityEventRepository
                    .findByEmpresaIdOrderByFechaInicioAsc(user.getEmpresaId());
            // Añade los globales activos que no sean ya de su empresa
            communityEventRepository.findByActivoTrueOrderByFechaInicioAsc()
                    .stream()
                    .filter(CommunityEvent::isEsGlobal)
                    .forEach(todos::add);
            return todos.stream().map(communityEventMapper::toResponse).collect(Collectors.toList());
        }

        // Empleado: su empresa + globales activos en una sola query optimizada
        return communityEventRepository.findVisiblesParaEmpresa(user.getEmpresaId())
                .stream().map(communityEventMapper::toResponse).collect(Collectors.toList());
    }


    // ── OBTENER POR ID ───────────────────────────────────────────────────────
    /**
     * Devuelve un evento por ID validando que el usuario tiene acceso
     */
    public CommunityEventResponse obtenerPorId(UUID eventoId, User user) {
        CommunityEvent evento = communityEventRepository.findById(eventoId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado: " + eventoId));

        validarAccesoLectura(evento, user);
        return communityEventMapper.toResponse(evento);
    }


    // ── ACTUALIZAR ───────────────────────────────────────────────────────────
    /**
     * Actualiza un evento existente
     * Solo superadmin puede cambiar el flag esGlobal
     * Admin empresa solo puede editar eventos de su propia empresa
     */
    @Transactional
    public CommunityEventResponse actualizar(UUID eventoId, CommunityEventRequest request, User user) {
        CommunityEvent evento = communityEventRepository.findById(eventoId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado: " + eventoId));

        validarAccesoEscritura(evento, user);

        evento.setTitulo(request.getTitulo());
        evento.setDescripcion(request.getDescripcion());
        evento.setFechaInicio(request.getFechaInicio());
        evento.setFechaFin(request.getFechaFin());
        evento.setLugar(request.getLugar());
        evento.setLatitud(request.getLatitud());
        evento.setLongitud(request.getLongitud());
        evento.setImagenUrl(request.getImagenUrl());

        // Solo superadmin puede promocionar o degradar un evento a global
        if (user.getRol().getRoleType() == RoleType.ROLE_ADMIN) {
            evento.setEsGlobal(request.isEsGlobal());
        }

        log.info("Evento actualizado: {} por usuario: {}", eventoId, user.getEmail());
        return communityEventMapper.toResponse(communityEventRepository.save(evento));
    }


    // ── DESACTIVAR ───────────────────────────────────────────────────────────
    /**
     * Soft delete del evento
     * Admin empresa solo puede desactivar eventos de su empresa
     * Lanza IllegalStateException si ya estaba desactivado - el GlobalExceptionHandler lo convierte en 400
     */
    @Transactional
    public CommunityEventResponse desactivar(UUID eventoId, User user) {
        CommunityEvent evento = communityEventRepository.findById(eventoId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado: " + eventoId));

        validarAccesoEscritura(evento, user);

        if (!evento.isActivo()) {
            throw new IllegalStateException("El evento ya está desactivado");
        }

        evento.setActivo(false);
        log.info("Evento desactivado: {} por usuario: {}", eventoId, user.getEmail());
        return communityEventMapper.toResponse(communityEventRepository.save(evento));
    }


    // ── VALIDACIONES DE ACCESO ───────────────────────────────────────────────

    private void validarAccesoLectura(CommunityEvent evento, User user) {
        RoleType rol = user.getRol().getRoleType();
        if (rol == RoleType.ROLE_ADMIN) return;

        boolean esGlobal = evento.isEsGlobal();
        boolean esDeSuEmpresa = user.getEmpresaId().equals(evento.getEmpresaId());

        if (!esGlobal && !esDeSuEmpresa) {
            throw new UnauthorizedException("No tienes acceso a este evento");
        }
    }

    private void validarAccesoEscritura(CommunityEvent evento, User user) {
        RoleType rol = user.getRol().getRoleType();
        if (rol == RoleType.ROLE_ADMIN) return;

        if (evento.isEsGlobal()) {
            throw new UnauthorizedException("Solo el superadmin puede modificar eventos globales");
        }

        if (!user.getEmpresaId().equals(evento.getEmpresaId())) {
            throw new UnauthorizedException("No puedes modificar eventos de otra empresa");
        }
    }
}