package com.atalayas.backend.communication.service;

import com.atalayas.backend.communication.dto.NotificationRequest;
import com.atalayas.backend.communication.dto.NotificationResponse;
import com.atalayas.backend.communication.entity.Notification;
import com.atalayas.backend.communication.mapper.NotificationMapper;
import com.atalayas.backend.communication.repository.NotificationRepository;
import com.atalayas.backend.exception.BusinessException;
import com.atalayas.backend.exception.ResourceNotFoundException;
import com.atalayas.backend.usuario.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lógica de negocio para las notificaciones individuales por usuario
 *
 * Hay dos formas de crear notificaciones:
 *   1. Interna (crearInterna), llamada desde otros servicios cuando ocurre
 *      un evento: módulo publicado, contenido completado, bienvenida, etc.
 *      No tiene endpoint propio — es API privada del backend
 *   2. Manual (crear), un admin la crea explícitamente para avisar a alguien.
 *      Tiene endpoint POST /api/v1/notificaciones
 *
 * Seguridad: cada usuario solo puede ver y marcar sus propias notificaciones
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;


    // ── USO INTERNO — llamado desde otros servicios ───────────────────────
    /**
     * Crea una notificación de forma programática cuando ocurre un evento.
     */
    @Transactional
    public NotificationResponse crearInterna(UUID destinatarioId, String tipo,
                                             String mensaje, String enlace) {
        Notification notification = Notification.builder()
                .destinatarioId(destinatarioId)
                .tipo(tipo)
                .mensaje(mensaje)
                .enlace(enlace)
                .leido(false)
                .build();

        notification = notificationRepository.save(notification);
        log.info("Notificación interna creada - id={} destinatario={} tipo={}",
                notification.getNotificacionId(), destinatarioId, tipo);
        return notificationMapper.toResponse(notification);
    }


    // ── ENDPOINTS PÚBLICOS ────────────────────────────────────────────────
    /**
     * Crea una notificación manual — solo ROLE_ADMIN y ROLE_ADMIN_EMPRESA.
     */
    @Transactional
    public NotificationResponse crear(NotificationRequest request, User user) {
        Notification notification = notificationMapper.toEntity(request);
        notification = notificationRepository.save(notification);
        log.info("Notificación manual creada - id={} por usuarioId={} para destinatarioId={}",
                notification.getNotificacionId(), user.getUsuarioId(), request.getDestinatarioId());
        return notificationMapper.toResponse(notification);
    }

    /**
     * Devuelve todas las notificaciones del usuario autenticado,
     * leídas y no leídas, ordenadas de más reciente a más antigua.
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> listarMias(User user) {
        return notificationRepository
                .findByDestinatarioIdOrderByCreadoEnDesc(user.getUsuarioId())
                .stream()
                .map(notificationMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Versión paginada de listarMias — para el panel con scroll infinito.
     * El frontend puede llamar ?page=0&size=20 e ir incrementando page.
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> listarMiasPaginado(User user, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("creadoEn").descending());
        return notificationRepository
                .findByDestinatarioIdOrderByCreadoEnDesc(user.getUsuarioId(), pageable)
                .map(notificationMapper::toResponse);
    }

    /**
     * Devuelve solo las notificaciones no leídas del usuario.
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> listarMisNoLeidas(User user) {
        return notificationRepository
                .findByDestinatarioIdAndLeidoFalseOrderByCreadoEnDesc(user.getUsuarioId())
                .stream()
                .map(notificationMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Devuelve el número de notificaciones no leídas del usuario.
     */
    @Transactional(readOnly = true)
    public long contarNoLeidas(User user) {
        return notificationRepository.countByDestinatarioIdAndLeidoFalse(user.getUsuarioId());
    }

    /**
     * Marca una notificación concreta como leída.
     * Lanza 404 si no existe, 400 si ya estaba leída o no es el destinatario.
     */
    @Transactional
    public NotificationResponse marcarComoLeida(UUID id, User user) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notificación no encontrada con id: " + id));

        // Solo el destinatario puede marcar sus propias notificaciones
        if (!notification.getDestinatarioId().equals(user.getUsuarioId())) {
            throw new BusinessException("No tienes permiso para marcar esta notificación");
        }

        if (notification.isLeido()) {
            throw new BusinessException("La notificación ya estaba marcada como leída");
        }

        notification.setLeido(true);
        notification = notificationRepository.save(notification);
        log.info("Notificación marcada como leída - id={} por usuarioId={}", id, user.getUsuarioId());
        return notificationMapper.toResponse(notification);
    }

    /**
     * Marca todas las notificaciones no leídas del usuario como leídas de golpe.
     * Devuelve cuántas se actualizaron para confirmación.
     */
    @Transactional
    public int marcarTodasComoLeidas(User user) {
        int actualizadas = notificationRepository.marcarTodasComoLeidas(user.getUsuarioId());
        log.info("Marcadas {} notificaciones como leídas para usuarioId={}",
                actualizadas, user.getUsuarioId());
        return actualizadas;
    }
}