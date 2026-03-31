package com.atalayas.backend.module.service;

import com.atalayas.backend.common.enums.RoleType;
import com.atalayas.backend.exception.ResourceNotFoundException;
import com.atalayas.backend.exception.UnauthorizedException;
import com.atalayas.backend.module.dto.ModuleRequest;
import com.atalayas.backend.module.dto.ModuleResponse;
import com.atalayas.backend.module.entity.TrainingModule;
import com.atalayas.backend.module.mapper.ModuleMapper;
import com.atalayas.backend.module.repository.ModuleRepository;
import com.atalayas.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * Lógica de negocio para módulos formativos
 *
 * Reglas de acceso:
 *   - ROLE_ADMIN         - acceso total, puede crear módulos globales
 *   - ROLE_ADMIN_EMPRESA - solo gestiona módulos de su empresa
 *   - ROLE_EMPLEADO      - solo lectura de módulos activos de su empresa + globales
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final ModuleMapper moduleMapper;


    // ── CREAR ────────────────────────────────────────────────────────────────
    /**
     * Crea un nuevo módulo
     * Admin empresa no puede crear módulos globales - su empresaId se fuerza siempre
     * Superadmin puede crear módulos globales dejando empresaId como null
     */
    @Transactional
    public ModuleResponse crear(ModuleRequest request, User user) {
        RoleType rol = user.getRol().getRoleType();

        UUID empresaId = (rol == RoleType.ROLE_ADMIN_EMPRESA)
                ? user.getEmpresaId()
                : request.getEmpresaId();

        TrainingModule guardado = moduleRepository.save(moduleMapper.toEntity(request, empresaId));
        log.info("Módulo creado: {} por usuario: {}", guardado.getModuloId(), user.getEmail());

        return moduleMapper.toResponse(guardado);
    }


    // ── LISTAR ───────────────────────────────────────────────────────────────
    /**
     * Devuelve los módulos visibles según el rol:
     *   - Superadmin: todos los activos de la plataforma
     *   - Admin empresa: todos los de su empresa + globales activos
     *   - Empleado: activos de su empresa + globales activos
     */
    public List<ModuleResponse> listar(User user) {
        RoleType rol = user.getRol().getRoleType();

        if (rol == RoleType.ROLE_ADMIN) {
            return moduleRepository.findByActivoTrueOrderByOrdenAsc()
                    .stream()
                    .map(moduleMapper::toResponse)
                    .collect(Collectors.toList());
        }

        List<TrainingModule> modulos = new ArrayList<>();

        if (rol == RoleType.ROLE_ADMIN_EMPRESA) {
            modulos.addAll(moduleRepository.findByEmpresaIdOrderByOrdenAsc(user.getEmpresaId()));
        } else {
            modulos.addAll(moduleRepository.findByEmpresaIdAndActivoTrueOrderByOrdenAsc(user.getEmpresaId()));
        }

        modulos.addAll(moduleRepository.findByEmpresaIdIsNullAndActivoTrueOrderByOrdenAsc());

        return modulos.stream()
                .map(moduleMapper::toResponse)
                .collect(Collectors.toList());
    }


    // ── OBTENER POR ID ───────────────────────────────────────────────────────
    /**
     * Devuelve un módulo por ID
     * Empleado y admin empresa solo pueden ver módulos de su empresa o globales
     */
    public ModuleResponse obtenerPorId(UUID moduloId, User user) {
        TrainingModule modulo = moduleRepository.findById(moduloId)
                .orElseThrow(() -> new ResourceNotFoundException("Módulo no encontrado: " + moduloId));

        validarAccesoLectura(modulo, user);
        return moduleMapper.toResponse(modulo);
    }


    // ── ACTUALIZAR ───────────────────────────────────────────────────────────
    /**
     * Actualiza un módulo existente
     * Admin empresa solo puede modificar módulos de su propia empresa
     */
    @Transactional
    public ModuleResponse actualizar(UUID moduloId, ModuleRequest request, User user) {
        TrainingModule modulo = moduleRepository.findById(moduloId)
                .orElseThrow(() -> new ResourceNotFoundException("Módulo no encontrado: " + moduloId));

        validarAccesoEscritura(modulo, user);

        modulo.setNombre(request.getNombre());
        modulo.setDescripcion(request.getDescripcion());
        modulo.setTipoModulo(request.getTipoModulo());
        modulo.setOrden(request.getOrden());
        modulo.setEsEspecializadoIa(request.isEsEspecializadoIa());
        modulo.setActivo(request.isActivo());

        log.info("Módulo actualizado: {} por usuario: {}", moduloId, user.getEmail());
        return moduleMapper.toResponse(moduleRepository.save(modulo));
    }


    // ── DESACTIVAR ───────────────────────────────────────────────────────────
    /**
     * Soft delete: marca el módulo como inactivo sin borrar datos históricos
     * Admin empresa solo puede desactivar los módulos de su empresa
     * Lanza IllegalStateException si ya estaba desactivado - GlobalExceptionHandler lo convierte en 400
     */
    @Transactional
    public ModuleResponse desactivar(UUID moduloId, User user) {
        TrainingModule modulo = moduleRepository.findById(moduloId)
                .orElseThrow(() -> new ResourceNotFoundException("Módulo no encontrado: " + moduloId));

        validarAccesoEscritura(modulo, user);

        if (!modulo.isActivo()) {
            throw new IllegalStateException("El módulo ya está desactivado");
        }

        modulo.setActivo(false);
        log.info("Módulo desactivado: {} por usuario: {}", moduloId, user.getEmail());
        return moduleMapper.toResponse(moduleRepository.save(modulo));
    }


    // ── VALIDACIONES DE ACCESO ───────────────────────────────────────────────
    /**
     * Verifica que el usuario puede leer este módulo
     * Es accesible si es global (empresaId null) o pertenece a su empresa
     */
    private void validarAccesoLectura(TrainingModule modulo, User user) {
        RoleType rol = user.getRol().getRoleType();
        if (rol == RoleType.ROLE_ADMIN) return;

        boolean esGlobal = modulo.getEmpresaId() == null;
        boolean esDeSuEmpresa = user.getEmpresaId().equals(modulo.getEmpresaId());

        if (!esGlobal && !esDeSuEmpresa) {
            throw new UnauthorizedException("No tienes acceso a este módulo");
        }
    }


    /**
     * Verifica que el usuario puede modificar este módulo
     * Solo superadmin puede tocar módulos globales (empresaId null)
     */
    private void validarAccesoEscritura(TrainingModule modulo, User user) {
        RoleType rol = user.getRol().getRoleType();
        if (rol == RoleType.ROLE_ADMIN) return;

        if (modulo.getEmpresaId() == null) {
            throw new UnauthorizedException("Solo el superadmin puede modificar módulos globales");
        }

        if (!user.getEmpresaId().equals(modulo.getEmpresaId())) {
            throw new UnauthorizedException("No puedes modificar módulos de otra empresa");
        }
    }
}