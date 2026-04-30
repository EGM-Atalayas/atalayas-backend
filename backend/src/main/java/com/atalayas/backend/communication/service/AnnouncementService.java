package com.atalayas.backend.communication.service;

import com.atalayas.backend.communication.dto.AnnouncementRequest;
import com.atalayas.backend.communication.dto.AnnouncementResponse;
import com.atalayas.backend.communication.entity.Announcement;
import com.atalayas.backend.communication.mapper.AnnouncementMapper;
import com.atalayas.backend.communication.repository.AnnouncementRepository;
import com.atalayas.backend.exception.BusinessException;
import com.atalayas.backend.exception.ResourceNotFoundException;
import com.atalayas.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Lógica de negocio para la gestión de anuncios de empresa
 * Los anuncios son comunicaciones de empresa a empleados, distintos de los
 * comunicados oficiales de EGM (Comunicado), que son globales y solo los
 * crea el superadmin.
 * Reglas de acceso:
 *   - ROLE_ADMIN         - puede crear anuncios globales y gestionar todos
 *   - ROLE_ADMIN_EMPRESA - solo puede crear y gestionar anuncios de su empresa
 *   - ROLE_EMPLEADO      - solo puede leer los anuncios de su empresa y los globales
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementMapper announcementMapper;

    // ── CREAR ─────────────────────────────────────────────────────────────
    /**
     * Crea un nuevo anuncio
     * Solo el superadmin puede marcar un anuncio como global.
     * Si un admin empresa intenta crear un anuncio global, el flag se ignora
     * y se fuerza a false.
     */
    @Transactional
    public AnnouncementResponse crear(AnnouncementRequest request, User user) {
        boolean isSuperAdmin = isSuperAdmin(user);

        // Resolvemos si el anuncio es global antes de pasarlo al mapper
        boolean esGlobal = isSuperAdmin && request.isEsGlobal();

        Announcement announcement = announcementMapper.toEntity(request, user, esGlobal);
        announcement = announcementRepository.save(announcement);
        return announcementMapper.toResponse(announcement);
    }

    // ── LISTAR ────────────────────────────────────────────────────────────
    /**
     * Lista los anuncios visibles para el usuario autenticado
     * La visibilidad depende del rol y la empresa del usuario:
     *   - Superadmin       - todos los anuncios activos de la plataforma
     *   - Con empresa      - los de su empresa + los globales activos
     *   - Sin empresa      - solo los globales activos (caso borde defensivo)
     */
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> listar(User user) {
        // Sin sesión activa — solo anuncios globales activos (endpoint público)
        if (user == null) {
            log.debug("Listando anuncios - acceso anónimo, devolviendo solo globales");
            return announcementRepository.findAllByEsGlobalTrueAndActivoTrue()
                    .stream().map(announcementMapper::toResponse).toList();
        }

        boolean superAdmin = isSuperAdmin(user);
        UUID empresaId = user.getEmpresaId();

        log.debug("Listando anuncios - usuarioId={} superAdmin={} empresaId={}",
                user.getUsuarioId(), superAdmin, empresaId);

        List<Announcement> announcements;

        if (superAdmin) {
            announcements = announcementRepository.findAllByActivoTrue();
        } else if (empresaId != null) {
            announcements = announcementRepository.findVisiblesParaEmpresa(empresaId);
        } else {
            // Caso defensivo: usuario sin empresa asignada — solo ve globales
            log.warn("Usuario {} no tiene empresaId - devolviendo solo anuncios globales",
                    user.getUsuarioId());
            announcements = announcementRepository.findAllByEsGlobalTrueAndActivoTrue();
        }

        return announcements.stream()
                .map(announcementMapper::toResponse)
                .toList();
    }

    // ── EDITAR ────────────────────────────────────────────────────────────
    /**
     * Edita un anuncio existente.
     * Superadmin puede editar cualquier anuncio.
     * Admin empresa solo puede editar los suyos propios.
     * Los campos de identidad (esGlobal, empresaId, creadoPor) no se modifican.
     */
    @Transactional
    public AnnouncementResponse editar(UUID id, AnnouncementRequest request, User user) {
        Announcement announcement;

        if (isSuperAdmin(user)) {
            announcement = announcementRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Anuncio no encontrado con id: " + id));
        } else {
            announcement = announcementRepository
                    .findByAnuncioIdAndEmpresaId(id, user.getEmpresaId())
                    .orElseThrow(() -> {
                        if (announcementRepository.existsById(id)) {
                            return new AccessDeniedException(
                                    "No tienes permisos para editar este anuncio");
                        }
                        return new ResourceNotFoundException(
                                "Anuncio no encontrado con id: " + id);
                    });
        }

        announcementMapper.updateEntity(announcement, request);
        announcement = announcementRepository.save(announcement);
        return announcementMapper.toResponse(announcement);
    }

    // ── REGISTRAR VISTA ───────────────────────────────────────────────────
    /**
     * Incrementa el contador de vistas de un anuncio en 1.
     * Operación no crítica — si el anuncio no existe simplemente se ignora.
     */
    @Transactional
    public void registrarVista(UUID id) {
        announcementRepository.findById(id).ifPresent(a -> {
            a.setVistas(a.getVistas() + 1);
            announcementRepository.save(a);
        });
    }

    // ── DESACTIVAR ────────────────────────────────────────────────────────
    /**
     * Soft-delete de un anuncio, marca activo = false sin eliminar el registro
     * Superadmin puede desactivar cualquier anuncio.
     * Admin empresa solo puede desactivar los suyos propios:
     *   - 403 si intenta desactivar uno global o de otra empresa
     *   - 404 si el anuncio no existe en absoluto
     */
    @Transactional
    public AnnouncementResponse desactivar(UUID id, User user) {
        Announcement announcement;

        if (isSuperAdmin(user)) {
            announcement = announcementRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Anuncio no encontrado con id: " + id));
        } else {
            announcement = announcementRepository
                    .findByAnuncioIdAndEmpresaId(id, user.getEmpresaId())
                    .orElseThrow(() -> {
                        // Distinguimos 404 (no existe) de 403 (existe pero no es suyo)
                        if (announcementRepository.existsById(id)) {
                            return new AccessDeniedException(
                                    "No tienes permisos para desactivar este anuncio");
                        }
                        return new ResourceNotFoundException(
                                "Anuncio no encontrado con id: " + id);
                    });
        }

        if (!announcement.isActivo()) {
            throw new BusinessException("El anuncio ya está desactivado");
        }

        announcement.setActivo(false);
        announcement = announcementRepository.save(announcement);
        return announcementMapper.toResponse(announcement);
    }


    // ── HELPERS ───────────────────────────────────────────────────────────
    private boolean isSuperAdmin(User user) {
        return user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}