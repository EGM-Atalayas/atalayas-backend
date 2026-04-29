package com.atalayas.backend.communication.service;

import com.atalayas.backend.communication.dto.OfficialNoticeRequest;
import com.atalayas.backend.communication.dto.OfficialNoticeResponse;
import com.atalayas.backend.communication.entity.OfficialNotice;
import com.atalayas.backend.communication.mapper.OfficialNoticeMapper;
import com.atalayas.backend.communication.repository.OfficialNoticeRepository;
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

/**
 * Lógica de negocio para los comunicados oficiales de EGM Atalayas
 *
 * Los comunicados son distintos de los anuncios de empresa (Announcement):
 *   - OfficialNotice: publicados por EGM, visibles para toda la plataforma
 *   - Announcement:   publicados por cada empresa, visibles solo para sus empleados
 *
 * Reglas de acceso:
 *   - ROLE_ADMIN         - puede crear, listar histórico completo y desactivar
 *   - ROLE_ADMIN_EMPRESA - solo ve los comunicados activos y vigentes
 *   - ROLE_EMPLEADO      - solo ve los comunicados activos y vigentes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfficialNoticeService {

    private final OfficialNoticeRepository noticeRepository;
    private final OfficialNoticeMapper noticeMapper;


    // ── CREAR ─────────────────────────────────────────────────────────────
    /**
     * Crea un nuevo comunicado oficial de EGM.
     * Solo accesible para ROLE_ADMIN.
     */
    @Transactional
    public OfficialNoticeResponse crear(OfficialNoticeRequest request, User user) {
        OfficialNotice notice = noticeMapper.toEntity(request, user);
        notice = noticeRepository.save(notice);
        log.info("Comunicado oficial creado - id={} por usuarioId={}",
                notice.getComunicadoId(), user.getUsuarioId());
        return noticeMapper.toResponse(notice);
    }


    // ── LISTAR ────────────────────────────────────────────────────────────
    /**
     * Lista los comunicados visibles según el rol del usuario.
     */
    @Transactional(readOnly = true)
    public List<OfficialNoticeResponse> listar(User user) {
        boolean esSuperAdmin = isSuperAdmin(user);

        log.debug("Listando comunicados - usuarioId={} esSuperAdmin={}",
                user.getUsuarioId(), esSuperAdmin);

        List<OfficialNotice> notices = esSuperAdmin
                ? noticeRepository.findAllByOrderByFechaPublicacionDesc()
                : noticeRepository.findActivosVigentes();

        return notices.stream()
                .map(noticeMapper::toResponse)
                .collect(Collectors.toList());
    }


    // ── ACTUALIZAR ───────────────────────────────────────────────────────
    /**
     * Actualiza los campos de un comunicado existente.
     * Solo accesible para ROLE_ADMIN.
     */
    @Transactional
    public OfficialNoticeResponse actualizar(UUID id, OfficialNoticeRequest request, User user) {
        OfficialNotice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Comunicado no encontrado con id: " + id));

        notice.setTitulo(request.getTitulo());
        notice.setMensaje(request.getMensaje());
        notice.setImagenUrl(request.getImagenUrl());
        notice.setCategoria(request.getCategoria());
        if (request.getDestacado() != null) notice.setDestacado(request.getDestacado());
        if (request.getEstado() != null)    notice.setEstado(request.getEstado());
        notice.setEnlaceUrl(request.getEnlaceUrl());
        notice.setEnlaceTexto(request.getEnlaceTexto());
        notice.setVideoUrl(request.getVideoUrl());
        notice.setAdjuntoUrl(request.getAdjuntoUrl());
        notice.setAdjuntoNombre(request.getAdjuntoNombre());
        notice.setFechaPublicacion(request.getFechaPublicacion());
        notice.setFechaExpiracion(request.getFechaExpiracion());

        notice = noticeRepository.save(notice);
        log.info("Comunicado actualizado - id={} por usuarioId={}", id, user.getUsuarioId());
        return noticeMapper.toResponse(notice);
    }


    // ── DESACTIVAR ────────────────────────────────────────────────────────
    /**
     * Soft-delete de un comunicado, marca activo = false sin borrar el registro.
     * Solo ROLE_ADMIN puede desactivar comunicados oficiales.
     */
    @Transactional
    public OfficialNoticeResponse desactivar(UUID id, User user) {
        OfficialNotice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Comunicado no encontrado con id: " + id));

        if (!notice.isActivo()) {
            throw new BusinessException("El comunicado ya está desactivado");
        }

        notice.setActivo(false);
        notice = noticeRepository.save(notice);
        log.info("Comunicado desactivado - id={} por usuarioId={}", id, user.getUsuarioId());
        return noticeMapper.toResponse(notice);
    }


    // ── HELPERS ───────────────────────────────────────────────────────────
    private boolean isSuperAdmin(User user) {
        return user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}