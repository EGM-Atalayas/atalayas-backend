package com.atalayas.backend.module.service;

import com.atalayas.backend.common.enums.ModuleType;
import com.atalayas.backend.exception.ResourceNotFoundException;
import com.atalayas.backend.exception.UnauthorizedException;
import com.atalayas.backend.module.dto.ModuleRequest;
import com.atalayas.backend.module.dto.ModuleResponse;
import com.atalayas.backend.module.entity.TrainingModule;
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
 *   - ROLE_ADMIN           - acceso total, puede crear módulos globales
 *   - ROLE_ADMIN_EMPRESA   - solo gestiona módulos de su empresa
 *   - ROLE_EMPLEADO        - solo lectura de módulos activos de su empresa + globales
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ModuleService {

    private final ModuleRepository moduleRepository;


    // ── CREAR ────────────────────────────────────────────────────────────────
    /**
     * Crea un nuevo módulo
     * Admin empresa no puede crear módulos globales: se fuerza su empresaId
     */
    @Transactional
    public ModuleResponse crear(ModuleRequest request, User user) {
        String rol = user.getRol().getCodigoRol();

        // Admin empresa siempre crea en su propia empresa, nunca global
        UUID empresaId = request.getEmpresaId();
        if ("ROLE_ADMIN_EMPRESA".equals(rol)) {
            empresaId = user.getEmpresaId();
        }

        TrainingModule modulo = TrainingModule.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .empresaId(empresaId)
                .tipoModulo(request.getTipoModulo())
                .orden(request.getOrden())
                .esEspecializadoIa(request.isEsEspecializadoIa())
                .activo(request.isActivo())
                .build();

        TrainingModule guardado = moduleRepository.save(modulo);
        log.info("Módulo creado: {} por usuario: {}", guardado.getModuloId(), user.getEmail());

        return toResponse(guardado);
    }


    // ── LISTAR ───────────────────────────────────────────────────────────────
    /**
     * Devuelve los módulos visibles según el rol del usuario:
     *   - Superadmin: todos los módulos activos de la plataforma
     *   - Admin empresa: módulos de su empresa (activos e inactivos) + globales activos
     *   - Empleado: módulos activos de su empresa + globales activos
     */
    public List<ModuleResponse> listar(User user) {
        String rol = user.getRol().getCodigoRol();

        if ("ROLE_ADMIN".equals(rol)) {
            // Superadmin ve todo lo activo
            return moduleRepository.findByActivoTrueOrderByOrdenAsc()
                    .stream().map(this::toResponse).collect(Collectors.toList());
        }

        // Admin empresa y empleados ven su empresa + globales
        List<TrainingModule> modulos = new ArrayList<>();

        if ("ROLE_ADMIN_EMPRESA".equals(rol)) {
            // Admin ve todos (activos e inactivos) de su empresa para gestionar
            modulos.addAll(moduleRepository.findByEmpresaIdOrderByOrdenAsc(user.getEmpresaId()));
        } else {
            // Empleado solo ve los activos
            modulos.addAll(moduleRepository.findByEmpresaIdAndActivoTrueOrderByOrdenAsc(user.getEmpresaId()));
        }

        // Todos ven los módulos globales activos
        modulos.addAll(moduleRepository.findByEmpresaIdIsNullAndActivoTrueOrderByOrdenAsc());

        return modulos.stream().map(this::toResponse).collect(Collectors.toList());
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
        return toResponse(modulo);
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
        return toResponse(moduleRepository.save(modulo));
    }

    // ── DESACTIVAR ───────────────────────────────────────────────────────────
    /**
     * Soft delete: marca el módulo como inactivo sin borrar datos históricos
     * Admin empresa solo puede desactivar los módulos de su empresa
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
        return toResponse(moduleRepository.save(modulo));
    }


    // ── VALIDACIONES DE ACCESO ───────────────────────────────────────────────
    /**
     * Verifica que el usuario puede leer este módulo
     * Un módulo es accesible si es global o pertenece a la empresa del usuario
     */
    private void validarAccesoLectura(TrainingModule modulo, User user) {
        String rol = user.getRol().getCodigoRol();
        if ("ROLE_ADMIN".equals(rol)) return; // Superadmin accede a todo

        boolean esGlobal = modulo.getEmpresaId() == null;
        boolean esDeSuEmpresa = user.getEmpresaId().equals(modulo.getEmpresaId());

        if (!esGlobal && !esDeSuEmpresa) {
            throw new UnauthorizedException("No tienes acceso a este módulo");
        }
    }


    /**
     * Verifica que el usuario puede modificar este módulo
     * Solo superadmin puede tocar módulos globales
     */
    private void validarAccesoEscritura(TrainingModule modulo, User user) {
        String rol = user.getRol().getCodigoRol();
        if ("ROLE_ADMIN".equals(rol)) return;

        if (modulo.getEmpresaId() == null) {
            throw new UnauthorizedException("Solo el superadmin puede modificar módulos globales");
        }

        if (!user.getEmpresaId().equals(modulo.getEmpresaId())) {
            throw new UnauthorizedException("No puedes modificar módulos de otra empresa");
        }
    }


    // ── MAPPER INTERNO ───────────────────────────────────────────────────────
    /**
     * Convierte la entidad a DTO de respuesta
     * El nombre de empresa se deja null por ahora - se pondrá
     * cuando integremos el CompanyService si el frontend lo necesita
     */
    private ModuleResponse toResponse(TrainingModule m) {
        return ModuleResponse.builder()
                .moduloId(m.getModuloId())
                .nombre(m.getNombre())
                .descripcion(m.getDescripcion())
                .empresaId(m.getEmpresaId())
                .tipoModulo(m.getTipoModulo())
                .orden(m.getOrden())
                .esEspecializadoIa(m.isEsEspecializadoIa())
                .activo(m.isActivo())
                .fechaCreacion(m.getFechaCreacion())
                .actualizadoEn(m.getActualizadoEn())
                .build();
    }
}