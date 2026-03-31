package com.atalayas.backend.communication.service;

import com.atalayas.backend.communication.dto.NotificacionRequest;
import com.atalayas.backend.communication.dto.NotificacionResponse;
import com.atalayas.backend.communication.entity.Notificacion;
import com.atalayas.backend.communication.mapper.NotificacionMapper;
import com.atalayas.backend.communication.repository.NotificacionRepository;
import com.atalayas.backend.exception.BusinessException;
import com.atalayas.backend.exception.ResourceNotFoundException;
import com.atalayas.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final NotificacionMapper notificacionMapper;


    // ── USO INTERNO (llamado desde otros servicios) ─────────────────────────
    /**
     * Crea una notificación de forma programática desde otros servicios
     * (ej. cuando se publica un módulo, se completa un progreso, etc.)
     * No expone endpoint — es API interna del backend
     */
    @Transactional
    public NotificacionResponse crearInterna(UUID destinatarioId, String tipo,
                                             String mensaje, String enlace) {
        Notificacion notificacion = Notificacion.builder()
                .destinatarioId(destinatarioId)
                .tipo(tipo)
                .mensaje(mensaje)
                .enlace(enlace)
                .leido(false)
                .build();

        notificacion = notificacionRepository.save(notificacion);
        log.info("Notificación interna creada - id={} destinatario={} tipo={}",
                notificacion.getNotificacionId(), destinatarioId, tipo);
        return notificacionMapper.toResponse(notificacion);
    }


    // ── ENDPOINTS PÚBLICOS ───────────────────────────────────────────────────
    /**
     * POST /api/v1/notificaciones
     * Creación manual de notificación por ROLE_ADMIN o ROLE_ADMIN_EMPRESA
     * ROLE_ADMIN_EMPRESA solo puede notificar a usuarios de su propia empresa
     * (la validación cross-company se delega al controller por ahora,
     *  ya que requiere acceso al UserRepository — se puede centralizar aquí si se necesita)
     */
    @Transactional
    public NotificacionResponse crear(NotificacionRequest request, User user) {
        Notificacion notificacion = notificacionMapper.toEntity(request);
        notificacion = notificacionRepository.save(notificacion);
        log.info("Notificación creada manualmente - id={} por usuarioId={} para destinatarioId={}",
                notificacion.getNotificacionId(), user.getUsuarioId(), request.getDestinatarioId());
        return notificacionMapper.toResponse(notificacion);
    }


    /**
     * GET /api/v1/notificaciones/me
     * El usuario autenticado ve todas sus notificaciones (leídas + no leídas)
     */
    @Transactional(readOnly = true)
    public List<NotificacionResponse> listarMias(User user) {
        return notificacionRepository
                .findByDestinatarioIdOrderByCreadoEnDesc(user.getUsuarioId())
                .stream()
                .map(notificacionMapper::toResponse)
                .collect(Collectors.toList());
    }


    /**
     * GET /api/v1/notificaciones/me/no-leidas
     * Solo las no leídas del usuario autenticado - para la campana del frontend
     */
    @Transactional(readOnly = true)
    public List<NotificacionResponse> listarMisNoLeidas(User user) {
        return notificacionRepository
                .findByDestinatarioIdAndLeidoFalseOrderByCreadoEnDesc(user.getUsuarioId())
                .stream()
                .map(notificacionMapper::toResponse)
                .collect(Collectors.toList());
    }


    /**
     * GET /api/v1/notificaciones/me/contador
     * Devuelve cuántas notificaciones no leídas tiene el usuario
     * Muy ligero — ideal para pooling periódico desde el frontend
     */
    @Transactional(readOnly = true)
    public long contarNoLeidas(User user) {
        return notificacionRepository.countByDestinatarioIdAndLeidoFalse(user.getUsuarioId());
    }


    /**
     * PATCH /api/v1/notificaciones/{id}/leer
     * Marca una notificación concreta como leída
     * Solo el propio destinatario puede marcarla (seguridad cross-user)
     * - 404 si no existe
     * - 403 si no es el destinatario
     * - 400 si ya estaba leída
     */
    @Transactional
    public NotificacionResponse marcarComoLeida(UUID id, User user) {
        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notificación no encontrada con id: " + id));

        // Seguridad cross-user: solo el destinatario puede marcarla
        if (!notificacion.getDestinatarioId().equals(user.getUsuarioId())) {
            throw new BusinessException("No tienes permiso para marcar esta notificación");
        }

        if (notificacion.isLeido()) {
            throw new BusinessException("La notificación ya estaba marcada como leída");
        }

        notificacion.setLeido(true);
        notificacion = notificacionRepository.save(notificacion);
        log.info("Notificación marcada como leída - id={} por usuarioId={}", id, user.getUsuarioId());
        return notificacionMapper.toResponse(notificacion);
    }


    /**
     * PATCH /api/v1/notificaciones/me/leer-todas
     * Marca todas las notificaciones no leídas del usuario como leídas de golpe
     * Devuelve cuántas se han actualizado
     */
    @Transactional
    public int marcarTodasComoLeidas(User user) {
        int actualizadas = notificacionRepository.marcarTodasComoLeidas(user.getUsuarioId());
        log.info("Marcadas {} notificaciones como leídas para usuarioId={}", actualizadas, user.getUsuarioId());
        return actualizadas;
    }
}