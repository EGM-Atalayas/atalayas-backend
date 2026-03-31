package com.atalayas.backend.progress.service;

import com.atalayas.backend.common.enums.ProgressStatus;
import com.atalayas.backend.exception.ResourceNotFoundException;
import com.atalayas.backend.exception.UnauthorizedException;
import com.atalayas.backend.progress.dto.CompleteContentRequest;
import com.atalayas.backend.progress.dto.ProgressResponse;
import com.atalayas.backend.progress.entity.UserProgress;
import com.atalayas.backend.progress.repository.ProgressRepository;
import com.atalayas.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * Lógica de negocio para trazabilidad de progreso formativo
 *
 * Reglas de acceso:
 *   - ROLE_EMPLEADO      - solo puede registrar y ver su propio progreso
 *   - ROLE_ADMIN_EMPRESA - ve el progreso de todos los empleados de su empresa
 *   - ROLE_ADMIN         - acceso total
 *
 * El estado (PENDIENTE / EN_PROGRESO / COMPLETADO) no se persiste en BD:
 * se deriva en cada respuesta a partir de 'completado' y 'tiempoSegundos'
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProgressService {

    private final ProgressRepository progressRepository;

    // ── REGISTRAR O ACTUALIZAR PROGRESO ──────────────────────────────────────
    /**
     * Registra o actualiza la trazabilidad de un empleado sobre un contenido
     *
     * Si ya existe un registro previo para ese (usuario, contenido) lo actualiza
     * Si no existe, lo crea, esto evita duplicados y permite acumular tiempo
     *
     * Un empleado solo puede registrar su propio progreso - no el de otros
     */
    @Transactional
    public ProgressResponse registrarProgreso(CompleteContentRequest request, User user) {
        String rol = user.getRol().getCodigoRol();

        // Empleado solo puede registrar su propio progreso
        if ("ROLE_EMPLEADO".equals(rol) && !user.getUsuarioId().equals(request.getUsuarioId())) {
            throw new UnauthorizedException("Solo puedes registrar tu propio progreso");
        }

        // Buscamos registro existente para hacer upsert
        UserProgress progreso = progressRepository
                .findByUsuarioIdAndContenidoId(request.getUsuarioId(), request.getContenidoId())
                .orElse(UserProgress.builder()
                        .usuarioId(request.getUsuarioId())
                        .contenidoId(request.getContenidoId())
                        .empresaId(request.getEmpresaId())
                        .build());

        // Acumulamos tiempo, frontend envía el tiempo de la sesión actual
        progreso.setTiempoSegundos(progreso.getTiempoSegundos() + request.getTiempoSegundos());
        progreso.setVersionLeida(request.getVersionLeida());
        progreso.setHashAceptacion(request.getHashAceptacion());

        // Completado es irreversible: una vez marcado no se puede desmarcar
        if (request.isCompletado() && !progreso.isCompletado()) {
            progreso.setCompletado(true);
            progreso.setFechaCompletado(LocalDateTime.now());
            log.info("Contenido {} completado por usuario {}", request.getContenidoId(), request.getUsuarioId());
        }

        return toResponse(progressRepository.save(progreso));
    }


    // ── PROGRESO PROPIO DEL EMPLEADO ─────────────────────────────────────────
    /**
     * Devuelve todo el progreso del usuario autenticado
     * Empleado solo ve el suyo - admin y admin empresa pueden ver el de cualquiera
     * dentro de su empresa
     */
    public List<ProgressResponse> miProgreso(User user) {
        return progressRepository
                .findByUsuarioIdOrderByActualizadoEnDesc(user.getUsuarioId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }


    // ── PROGRESO DE UN EMPLEADO CONCRETO (ADMIN) ─────────────────────────────
    /**
     * Devuelve el progreso de un empleado concreto
     * Admin empresa solo puede consultar empleados de su empresa
     */
    public List<ProgressResponse> progresoPorUsuario(UUID usuarioId, User user) {
        String rol = user.getRol().getCodigoRol();

        if ("ROLE_ADMIN".equals(rol)) {
            // Superadmin ve todo sin restricción
            return progressRepository
                    .findByUsuarioIdOrderByActualizadoEnDesc(usuarioId)
                    .stream().map(this::toResponse).collect(Collectors.toList());
        }

        // Admin empresa solo ve empleados de su empresa — seguridad cross-company
        return progressRepository
                .findByUsuarioIdAndEmpresaId(usuarioId, user.getEmpresaId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }


    // ── PROGRESO DE TODA LA EMPRESA (DASHBOARD ADMIN) ────────────────────────
    /**
     * Devuelve todo el progreso de los empleados de una empresa
     * Admin empresa solo puede consultar su propia empresa
     */
    public List<ProgressResponse> progresoPorEmpresa(UUID empresaId, User user) {
        String rol = user.getRol().getCodigoRol();

        // Admin empresa no puede consultar datos de otra empresa
        if ("ROLE_ADMIN_EMPRESA".equals(rol) && !user.getEmpresaId().equals(empresaId)) {
            throw new UnauthorizedException("Solo puedes consultar el progreso de tu empresa");
        }

        return progressRepository
                .findByEmpresaIdOrderByActualizadoEnDesc(empresaId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }


    // ── PROGRESO sSOBRE UN CONTENIDO CONCRETO ─────────────────────────────────
    /**
     * Devuelve el estado de un empleado sobre un contenido concreto
     * Útil para que el frontend sepa si mostrar el contenido como completado
     */
    public ProgressResponse progresoPorContenido(UUID contenidoId, User user) {
        return progressRepository
                .findByUsuarioIdAndContenidoId(user.getUsuarioId(), contenidoId)
                .map(this::toResponse)
                // Si no hay registro aún, devolvemos estado PENDIENTE virtual
                .orElse(ProgressResponse.builder()
                        .usuarioId(user.getUsuarioId())
                        .contenidoId(contenidoId)
                        .empresaId(user.getEmpresaId())
                        .completado(false)
                        .tiempoSegundos(0)
                        .estado(ProgressStatus.PENDIENTE)
                        .build());
    }


    // ── DERIVAR ESTADO DESDE CAMPOS PERSISTIDOS ──────────────────────────────
    /**
     * Calcula el estado de progreso a partir de los datos persistidos
     * Este campo NO se guarda en BD — se construye en cada respuesta
     *
     *   PENDIENTE   - nunca abierto (tiempoSegundos = 0 y no completado)
     *   EN_PROGRESO - ha empezado pero no ha marcado como completado
     *   COMPLETADO  - completado = true
     */
    private ProgressStatus derivarEstado(UserProgress p) {
        if (p.isCompletado()) return ProgressStatus.COMPLETADO;
        if (p.getTiempoSegundos() > 0) return ProgressStatus.EN_PROGRESO;
        return ProgressStatus.PENDIENTE;
    }


    // ── MAPPER INTERNO ───────────────────────────────────────────────────────

    private ProgressResponse toResponse(UserProgress p) {
        return ProgressResponse.builder()
                .registroId(p.getRegistroId())
                .usuarioId(p.getUsuarioId())
                .contenidoId(p.getContenidoId())
                .empresaId(p.getEmpresaId())
                .completado(p.isCompletado())
                .fechaCompletado(p.getFechaCompletado())
                .tiempoSegundos(p.getTiempoSegundos())
                .versionLeida(p.getVersionLeida())
                .hashAceptacion(p.getHashAceptacion())
                .estado(derivarEstado(p))
                .actualizadoEn(p.getActualizadoEn())
                .build();
    }
}