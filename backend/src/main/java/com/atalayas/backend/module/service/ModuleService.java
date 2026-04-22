package com.atalayas.backend.module.service;

import com.atalayas.backend.common.enums.RoleType;
import com.atalayas.backend.communication.service.NotificationService;
import com.atalayas.backend.exception.ResourceNotFoundException;
import com.atalayas.backend.exception.UnauthorizedException;
import com.atalayas.backend.module.dto.ModuleRequest;
import com.atalayas.backend.module.dto.ModuleResponse;
import com.atalayas.backend.module.entity.TrainingModule;
import com.atalayas.backend.module.mapper.ModuleMapper;
import com.atalayas.backend.module.repository.ModuleRepository;
import com.atalayas.backend.user.entity.User;
import com.atalayas.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lógica de negocio para módulos formativos.
 *
 * Reglas de acceso:
 *   - ROLE_ADMIN         → acceso total, puede crear módulos globales
 *   - ROLE_ADMIN_EMPRESA → solo gestiona módulos de su empresa
 *   - ROLE_EMPLEADO      → solo lectura de módulos activos de su empresa + globales
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final ModuleMapper moduleMapper;
    private final NotificationService notificationService;
    private final UserRepository userRepository;


    // ── CREAR ─────────────────────────────────────────────────────────────

    /**
     * Crea un nuevo módulo formativo.
     *
     * El admin empresa no puede crear módulos globales — su empresaId
     * se fuerza siempre desde el usuario autenticado.
     * El superadmin puede crear módulos globales dejando empresaId como null.
     *
     * Al crear un módulo de empresa se notifica a todos sus empleados activos.
     */
    @Transactional
    public ModuleResponse crear(ModuleRequest request, User user) {
        RoleType rol = user.getRol().getRoleType();

        UUID empresaId = (rol == RoleType.ROLE_ADMIN_EMPRESA)
                ? user.getEmpresaId()
                : request.getEmpresaId();

        TrainingModule modulo = moduleRepository.save(
                moduleMapper.toEntity(request, empresaId));
        log.info("Módulo creado — id={} por usuarioId={}", modulo.getModuloId(), user.getEmail());

        // Notificamos a los empleados activos de la empresa cuando se publica
        // un módulo nuevo. Los módulos globales los gestiona EGM directamente.
        if (empresaId != null) {
            userRepository.findAllByEmpresaIdAndActivoTrue(empresaId).forEach(empleado ->
                    notificationService.crearInterna(
                            empleado.getUsuarioId(),
                            "MODULO_NUEVO",
                            "Nuevo módulo disponible: \"" + modulo.getNombre() +
                                    "\". ¡Empieza cuando quieras!",
                            "/formacion/modulo/" + modulo.getModuloId()
                    )
            );
        }

        return moduleMapper.toResponse(modulo);
    }


    // ── LISTAR ────────────────────────────────────────────────────────────

    /**
     * Devuelve los módulos visibles según el rol del usuario.
     *   - Superadmin      → todos los activos de la plataforma
     *   - Admin empresa   → todos los de su empresa + globales activos
     *   - Empleado        → activos de su empresa + globales activos
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
            // Admin empresa ve todos los de su empresa, activos e inactivos
            modulos.addAll(moduleRepository
                    .findByEmpresaIdOrderByOrdenAsc(user.getEmpresaId()));
        } else {
            // Empleado solo ve los activos de su empresa
            modulos.addAll(moduleRepository
                    .findByEmpresaIdAndActivoTrueOrderByOrdenAsc(user.getEmpresaId()));
        }

        // Todos ven los módulos globales activos (empresaId = null)
        modulos.addAll(moduleRepository
                .findByEmpresaIdIsNullAndActivoTrueOrderByOrdenAsc());

        return modulos.stream()
                .map(moduleMapper::toResponse)
                .collect(Collectors.toList());
    }


    // ── OBTENER POR ID ────────────────────────────────────────────────────

    /**
     * Devuelve un módulo por ID.
     * Empleado y admin empresa solo pueden ver módulos de su empresa o globales.
     */
    public ModuleResponse obtenerPorId(UUID moduloId, User user) {
        TrainingModule modulo = moduleRepository.findById(moduloId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Módulo no encontrado: " + moduloId));

        validarAccesoLectura(modulo, user);
        return moduleMapper.toResponse(modulo);
    }


    // ── ACTUALIZAR ────────────────────────────────────────────────────────

    /**
     * Actualiza un módulo existente.
     * Admin empresa solo puede modificar módulos de su propia empresa.
     */
    @Transactional
    public ModuleResponse actualizar(UUID moduloId, ModuleRequest request, User user) {
        TrainingModule modulo = moduleRepository.findById(moduloId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Módulo no encontrado: " + moduloId));

        validarAccesoEscritura(modulo, user);

        modulo.setNombre(request.getNombre());
        modulo.setDescripcion(request.getDescripcion());
        modulo.setTipoModulo(request.getTipoModulo());
        modulo.setOrden(request.getOrden());
        modulo.setEsEspecializadoIa(request.isEsEspecializadoIa());
        modulo.setActivo(request.isActivo());
        if (request.getIdioma() != null)      modulo.setIdioma(request.getIdioma());
        if (request.getDuracion() != null)    modulo.setDuracion(request.getDuracion());
        if (request.getAudiencia() != null)   modulo.setAudiencia(request.getAudiencia());
        if (request.getDepartamentos() != null) modulo.setDepartamentos(request.getDepartamentos());
        if (request.getTestPreguntas() != null) modulo.setTestPreguntas(request.getTestPreguntas());

        log.info("Módulo actualizado — id={} por usuarioId={}", moduloId, user.getEmail());
        return moduleMapper.toResponse(moduleRepository.save(modulo));
    }


    // ── DESACTIVAR ────────────────────────────────────────────────────────

    /**
     * Soft-delete del módulo — lo marca como inactivo sin borrar datos históricos.
     * Admin empresa solo puede desactivar módulos de su empresa.
     * Lanza IllegalStateException si ya estaba desactivado — el GlobalExceptionHandler
     * lo convierte en 400.
     */
    @Transactional
    public ModuleResponse desactivar(UUID moduloId, User user) {
        TrainingModule modulo = moduleRepository.findById(moduloId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Módulo no encontrado: " + moduloId));

        validarAccesoEscritura(modulo, user);

        if (!modulo.isActivo()) {
            throw new IllegalStateException("El módulo ya está desactivado");
        }

        modulo.setActivo(false);
        log.info("Módulo desactivado — id={} por usuarioId={}", moduloId, user.getEmail());
        return moduleMapper.toResponse(moduleRepository.save(modulo));
    }


    // ── VALIDACIONES DE ACCESO ────────────────────────────────────────────

    /**
     * Verifica que el usuario puede leer este módulo.
     * Es accesible si es global (empresaId = null) o pertenece a su empresa.
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
     * Verifica que el usuario puede modificar este módulo.
     * Solo el superadmin puede tocar módulos globales (empresaId = null).
     */
    private void validarAccesoEscritura(TrainingModule modulo, User user) {
        RoleType rol = user.getRol().getRoleType();
        if (rol == RoleType.ROLE_ADMIN) return;

        if (modulo.getEmpresaId() == null) {
            throw new UnauthorizedException(
                    "Solo el superadmin puede modificar módulos globales");
        }

        if (!user.getEmpresaId().equals(modulo.getEmpresaId())) {
            throw new UnauthorizedException(
                    "No puedes modificar módulos de otra empresa");
        }
    }
}