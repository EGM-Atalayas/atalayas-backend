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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementMapper announcementMapper;

    /**
     * POST /api/v1/anuncios — Crear anuncio.
     * - ROLE_ADMIN: puede crear anuncios globales (esGlobal=true, empresaId=null).
     * - ROLE_ADMIN_EMPRESA: el campo esGlobal del request se ignora y se fuerza a false;
     *   la empresa del anuncio se toma de su propio empresaId.
     */
    @Transactional
    public AnnouncementResponse crear(AnnouncementRequest request, User user) {
        boolean isSuperAdmin = isSuperAdmin(user);
        // Prevención de escalada de privilegios: solo el superadmin puede marcar global
        boolean esGlobal = isSuperAdmin && request.isEsGlobal();

        Announcement announcement = announcementMapper.toEntity(request, user, esGlobal);
        announcement = announcementRepository.save(announcement);
        return announcementMapper.toResponse(announcement);
    }

    /**
     * GET /api/v1/anuncios — Listar anuncios visibles para el usuario.
     * - ROLE_ADMIN: todos los activos de la plataforma.
     * - Cualquier otro rol: los de su empresa + los globales activos.
     */
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> listar(User user) {
        List<Announcement> announcements = isSuperAdmin(user)
                ? announcementRepository.findAllByActivoTrue()
                : announcementRepository.findVisiblesParaEmpresa(user.getEmpresaId());

        return announcements.stream()
                .map(announcementMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * PATCH /api/v1/anuncios/{id}/desactivar — Soft-delete (activo = false).
     * - ROLE_ADMIN: puede desactivar cualquier anuncio.
     * - ROLE_ADMIN_EMPRESA: solo puede desactivar los propios (empresa_id coincide).
     *   → 403 si intenta desactivar uno global o de otra empresa.
     *   → 404 si el anuncio no existe en absoluto.
     */
    @Transactional
    public AnnouncementResponse desactivar(UUID id, User user) {
        Announcement announcement;

        if (isSuperAdmin(user)) {
            announcement = announcementRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Anuncio no encontrado con id: " + id));
        } else {
            // ROLE_ADMIN_EMPRESA: buscar validando propiedad en una sola query
            announcement = announcementRepository
                    .findByAnuncioIdAndEmpresaId(id, user.getEmpresaId())
                    .orElseThrow(() -> {
                        // Distinguir 404 (no existe) de 403 (existe pero no es suyo)
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

    // ── Helpers ──────────────────────────────────────────────────────────────

    private boolean isSuperAdmin(User user) {
        return user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}

