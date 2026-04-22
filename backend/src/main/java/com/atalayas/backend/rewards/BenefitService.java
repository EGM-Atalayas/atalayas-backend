package com.atalayas.backend.rewards;

import com.atalayas.backend.common.enums.RoleType;
import com.atalayas.backend.exception.ResourceNotFoundException;
import com.atalayas.backend.exception.UnauthorizedException;
import com.atalayas.backend.rewards.dto.BenefitRequest;
import com.atalayas.backend.rewards.dto.BenefitResponse;
import com.atalayas.backend.rewards.entity.Benefit;
import com.atalayas.backend.rewards.mapper.BenefitMapper;
import com.atalayas.backend.rewards.repository.BenefitRepository;
import com.atalayas.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * Lógica de negocio para beneficios y ventajas del área empresarial
 *
 * Reglas de acceso:
 *   - ROLE_ADMIN         - acceso total, puede crear beneficios globales
 *   - ROLE_ADMIN_EMPRESA - gestiona beneficios de su empresa, no puede crear globales
 *   - ROLE_EMPLEADO      - solo lectura de beneficios activos de su empresa + globales
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BenefitService {

    private final BenefitRepository benefitRepository;
    private final BenefitMapper benefitMapper;


    // ── CREAR ────────────────────────────────────────────────────────────────
    /**
     * Crea un beneficio
     * Admin empresa siempre crea en su empresa — no puede crear beneficios globales
     * Superadmin puede crear beneficios globales dejando empresaId como null
     */
    @Transactional
    public BenefitResponse crear(BenefitRequest request, User user) {
        RoleType rol = user.getRol().getRoleType();

        UUID empresaId = (rol == RoleType.ROLE_ADMIN_EMPRESA)
                ? user.getEmpresaId()
                : request.getEmpresaId();

        Benefit guardado = benefitRepository.save(
                benefitMapper.toEntity(request, empresaId, user.getUsuarioId()));

        log.info("Beneficio creado: {} por usuario: {}", guardado.getBeneficioId(), user.getEmail());
        return benefitMapper.toResponse(guardado);
    }


    // ── LISTAR ───────────────────────────────────────────────────────────────
    /**
     * Devuelve los beneficios visibles según el rol:
     *   - Superadmin: todos los activos de la plataforma
     *   - Admin empresa y empleado: los de su empresa + globales activos
     */
    public List<BenefitResponse> listar(User user) {
        RoleType rol = user.getRol().getRoleType();

        if (rol == RoleType.ROLE_ADMIN) {
            return benefitRepository.findByActivoTrueOrderByCreadoEnDesc()
                    .stream().map(benefitMapper::toResponse).collect(Collectors.toList());
        }

        // Admin empresa y empleados comparten la misma query de visibilidad
        return benefitRepository.findVisiblesParaEmpresa(user.getEmpresaId())
                .stream().map(benefitMapper::toResponse).collect(Collectors.toList());
    }


    // ── ACTUALIZAR ───────────────────────────────────────────────────────────
    /**
     * Actualiza un beneficio existente
     * Admin empresa solo puede editar los de su empresa
     */
    @Transactional
    public BenefitResponse actualizar(UUID beneficioId, BenefitRequest request, User user) {
        Benefit beneficio = benefitRepository.findById(beneficioId)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficio no encontrado: " + beneficioId));

        validarAccesoEscritura(beneficio, user);

        beneficio.setTitulo(request.getTitulo());
        beneficio.setDescripcion(request.getDescripcion());
        beneficio.setUrlInfo(request.getUrlInfo());

        log.info("Beneficio actualizado: {} por usuario: {}", beneficioId, user.getEmail());
        return benefitMapper.toResponse(benefitRepository.save(beneficio));
    }


    // ── DESACTIVAR ───────────────────────────────────────────────────────────
    /**
     * Soft delete del beneficio
     * Admin empresa solo puede desactivar los de su empresa
     * Lanza IllegalStateException si ya estaba desactivado - el GlobalExceptionHandler lo convierte en 400
     */
    @Transactional
    public BenefitResponse desactivar(UUID beneficioId, User user) {
        Benefit beneficio = benefitRepository.findById(beneficioId)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficio no encontrado: " + beneficioId));

        validarAccesoEscritura(beneficio, user);

        if (!beneficio.isActivo()) {
            throw new IllegalStateException("El beneficio ya está desactivado");
        }

        beneficio.setActivo(false);
        log.info("Beneficio desactivado: {} por usuario: {}", beneficioId, user.getEmail());
        return benefitMapper.toResponse(benefitRepository.save(beneficio));
    }


    // ── VALIDACIONES DE ACCESO ───────────────────────────────────────────────

    private void validarAccesoEscritura(Benefit beneficio, User user) {
        RoleType rol = user.getRol().getRoleType();
        if (rol == RoleType.ROLE_ADMIN) return;

        if (beneficio.getEmpresaId() == null) {
            throw new UnauthorizedException("Solo el superadmin puede modificar beneficios globales");
        }

        if (!user.getEmpresaId().equals(beneficio.getEmpresaId())) {
            throw new UnauthorizedException("No puedes modificar beneficios de otra empresa");
        }
    }
}