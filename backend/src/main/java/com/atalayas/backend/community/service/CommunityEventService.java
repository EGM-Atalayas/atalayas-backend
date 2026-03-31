package com.atalayas.backend.community.service;

import com.atalayas.backend.community.dto.CommunityEventRequest;
import com.atalayas.backend.community.dto.CommunityEventResponse;
import com.atalayas.backend.community.entity.CommunityEvent;
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


    // ── CREAR ────────────────────────────────────────────────────────────────
    /**
     * Crea un evento de comunidad
     * Admin empresa siempre crea en su empresa y no puede crear globales
     * Superadmin puede crear eventos globales visibles para toda la plataforma
     */
    @Transactional
    public CommunityEventResponse crear(CommunityEventRequest request, User user) {
        String rol = user.getRol().getCodigoRol();

        // Admin empresa no puede crear eventos globales ni de otra empresa
        UUID empresaId = request.getEmpresaId();
        boolean esGlobal = request.isEsGlobal();

        if ("ROLE_ADMIN_EMPRESA".equals(rol)) {
            empresaId = user.getEmpresaId();
            esGlobal = false;
        }

        CommunityEvent evento = CommunityEvent.builder()
                .titulo(request.getTitulo())
                .descripcion(request.getDescripcion())
                .empresaId(empresaId)
                .esGlobal(esGlobal)
                .fechaInicio(request.getFechaInicio())
                .fechaFin(request.getFechaFin())
                .creadoPor(user.getUsuarioId())
                .activo(true)
                .build();

        CommunityEvent guardado = communityEventRepository.save(evento);
        log.info("Evento creado: {} por usuario: {}", guardado.getEventoId(), user.getEmail());

        return toResponse(guardado);
    }


    // ── LISTAR ───────────────────────────────────────────────────────────────
    /**
     * Devuelve los eventos visibles según el rol:
     *   - Superadmin: todos los activos de la plataforma
     *   - Admin empresa: los de su empresa (activos e inactivos) + globales activos
     *   - Empleado: activos de su empresa + globales activos
     */
    public List<CommunityEventResponse> listar(User user) {
        String rol = user.getRol().getCodigoRol();

        if ("ROLE_ADMIN".equals(rol)) {
            return communityEventRepository.findByActivoTrueOrderByFechaInicioAsc()
                    .stream().map(this::toResponse).collect(Collectors.toList());
        }

        if ("ROLE_ADMIN_EMPRESA".equals(rol)) {
            // Admin ve todos los de su empresa para gestionar + globales activos
            List<CommunityEvent> todos = communityEventRepository
                    .findByEmpresaIdOrderByFechaInicioAsc(user.getEmpresaId());
            List<CommunityEvent> globales = communityEventRepository
                    .findByActivoTrueOrderByFechaInicioAsc()
                    .stream().filter(CommunityEvent::isEsGlobal).collect(Collectors.toList());
            todos.addAll(globales);
            return todos.stream().map(this::toResponse).collect(Collectors.toList());
        }

        // Empleado: su empresa + globales activos en una sola query
        return communityEventRepository
                .findVisiblesParaEmpresa(user.getEmpresaId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }


    // ── OBTENER ID ───────────────────────────────────────────────────────
    /**
     * Devuelve un evento por ID validando que el usuario tiene acceso
     */
    public CommunityEventResponse obtenerPorId(UUID eventoId, User user) {
        CommunityEvent evento = communityEventRepository.findById(eventoId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado: " + eventoId));

        validarAccesoLectura(evento, user);
        return toResponse(evento);
    }


    // ── ACTUALIZAR ───────────────────────────────────────────────────────────
    /**
     * Actualiza un evento existente
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

        // Solo superadmin puede cambiar el flag global
        if ("ROLE_ADMIN".equals(user.getRol().getCodigoRol())) {
            evento.setEsGlobal(request.isEsGlobal());
        }

        log.info("Evento actualizado: {} por usuario: {}", eventoId, user.getEmail());
        return toResponse(communityEventRepository.save(evento));
    }


    // ── DESACTIVAR ───────────────────────────────────────────────────────────
    /**
     * Soft delete del evento
     * Admin empresa solo puede desactivar eventos de su empresa
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
        return toResponse(communityEventRepository.save(evento));
    }


    // ── VALIDACIONES DE ACCESO ───────────────────────────────────────────────

    private void validarAccesoLectura(CommunityEvent evento, User user) {
        String rol = user.getRol().getCodigoRol();
        if ("ROLE_ADMIN".equals(rol)) return;

        boolean esGlobal = evento.isEsGlobal();
        boolean esDeSuEmpresa = user.getEmpresaId().equals(evento.getEmpresaId());

        if (!esGlobal && !esDeSuEmpresa) {
            throw new UnauthorizedException("No tienes acceso a este evento");
        }
    }

    private void validarAccesoEscritura(CommunityEvent evento, User user) {
        String rol = user.getRol().getCodigoRol();
        if ("ROLE_ADMIN".equals(rol)) return;

        if (evento.isEsGlobal()) {
            throw new UnauthorizedException("Solo el superadmin puede modificar eventos globales");
        }

        if (!user.getEmpresaId().equals(evento.getEmpresaId())) {
            throw new UnauthorizedException("No puedes modificar eventos de otra empresa");
        }
    }


    // ── MAPPER INTERNO ───────────────────────────────────────────────────────

    private CommunityEventResponse toResponse(CommunityEvent e) {
        return CommunityEventResponse.builder()
                .eventoId(e.getEventoId())
                .empresaId(e.getEmpresaId())
                .creadoPor(e.getCreadoPor())
                .titulo(e.getTitulo())
                .descripcion(e.getDescripcion())
                .esGlobal(e.isEsGlobal())
                .activo(e.isActivo())
                .fechaInicio(e.getFechaInicio())
                .fechaFin(e.getFechaFin())
                .creadoEn(e.getCreadoEn())
                .actualizadoEn(e.getActualizadoEn())
                .build();
    }
}