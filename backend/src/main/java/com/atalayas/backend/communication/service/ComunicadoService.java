package com.atalayas.backend.communication.service;

import com.atalayas.backend.communication.dto.ComunicadoRequest;
import com.atalayas.backend.communication.dto.ComunicadoResponse;
import com.atalayas.backend.communication.entity.Comunicado;
import com.atalayas.backend.communication.mapper.ComunicadoMapper;
import com.atalayas.backend.communication.repository.ComunicadoRepository;
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
public class ComunicadoService {

    private final ComunicadoRepository comunicadoRepository;
    private final ComunicadoMapper comunicadoMapper;


    /**
     * POST /api/v1/comunicados — Crear comunicado oficial de EGM
     * Solo accesible para ROLE_ADMIN (controlado en el controller con @PreAuthorize)
     */
    @Transactional
    public ComunicadoResponse crear(ComunicadoRequest request, User user) {
        Comunicado comunicado = comunicadoMapper.toEntity(request, user);
        comunicado = comunicadoRepository.save(comunicado);
        log.info("Comunicado creado - id={} creadoPor={}", comunicado.getComunicadoId(), user.getUsuarioId());
        return comunicadoMapper.toResponse(comunicado);
    }


    /**
     * GET /api/v1/comunicados - Listar comunicados visibles
     * - ROLE_ADMIN:                    ve el histórico completo (incluyendo expirados y desactivados)
     * - ROLE_ADMIN_EMPRESA / EMPLEADO: solo los activos y vigentes
     */
    @Transactional(readOnly = true)
    public List<ComunicadoResponse> listar(User user) {
        boolean superAdmin = isSuperAdmin(user);

        log.debug("Listando comunicados - usuarioId={} superAdmin={}", user.getUsuarioId(), superAdmin);

        List<Comunicado> comunicados = superAdmin
                ? comunicadoRepository.findAllByOrderByFechaPublicacionDesc()
                : comunicadoRepository.findActivosVigentes();

        return comunicados.stream()
                .map(comunicadoMapper::toResponse)
                .collect(Collectors.toList());
    }


    /**
     * PATCH /api/v1/comunicados/{id}/desactivar - Soft-delete (activo = false)
     * Solo ROLE_ADMIN puede desactivar comunicados
     * → 404 si no existe
     * → 400 si ya estaba desactivado
     */
    @Transactional
    public ComunicadoResponse desactivar(UUID id, User user) {
        Comunicado comunicado = comunicadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Comunicado no encontrado con id: " + id));

        if (!comunicado.isActivo()) {
            throw new BusinessException("El comunicado ya está desactivado");
        }

        comunicado.setActivo(false);
        comunicado = comunicadoRepository.save(comunicado);
        log.info("Comunicado desactivado - id={} por usuarioId={}", id, user.getUsuarioId());
        return comunicadoMapper.toResponse(comunicado);
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private boolean isSuperAdmin(User user) {
        return user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}