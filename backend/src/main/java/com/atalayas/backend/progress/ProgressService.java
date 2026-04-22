package com.atalayas.backend.progress;

import com.atalayas.backend.common.enums.ProgressStatus;
import com.atalayas.backend.communication.service.NotificationService;
import com.atalayas.backend.content.entity.ContentItem;
import com.atalayas.backend.content.repository.ContentRepository;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lógica de negocio para trazabilidad de progreso formativo.
 *
 * Reglas de acceso:
 *   - ROLE_EMPLEADO      → solo puede registrar y ver su propio progreso
 *   - ROLE_ADMIN_EMPRESA → ve el progreso de todos los empleados de su empresa
 *   - ROLE_ADMIN         → acceso total sin restricción
 *
 * El estado (PENDIENTE / EN_PROGRESO / COMPLETADO) no se persiste en BD —
 * se deriva en cada respuesta a partir de 'completado' y 'tiempoSegundos'.
 *
 * moduloId se desnormaliza en trazabilidad_lectura para evitar joins
 * costosos en las queries de dashboard — lo rellenamos aquí al registrar.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProgressService {

    private final ProgressRepository progressRepository;
    private final ContentRepository contentRepository;
    private final NotificationService notificationService;


    // ── REGISTRAR O ACTUALIZAR PROGRESO ───────────────────────────────────

    /**
     * Registra o actualiza la trazabilidad de un empleado sobre un contenido.
     *
     * Si ya existe un registro para ese (usuario, contenido) lo actualiza.
     * Si no existe, lo crea — esto evita duplicados y permite acumular tiempo.
     *
     * Al crear el registro rellenamos moduloId desde el contenido para que
     * las queries de dashboard puedan filtrar por módulo sin joins adicionales.
     *
     * Cuando el contenido se completa por primera vez se dispara una notificación.
     */
    @Transactional
    public ProgressResponse registrarProgreso(CompleteContentRequest request, User user) {
        String rol = user.getRol().getCodigoRol();

        // Un empleado solo puede registrar su propio progreso, no el de otros
        if ("ROLE_EMPLEADO".equals(rol)
                && !user.getUsuarioId().equals(request.getUsuarioId())) {
            throw new UnauthorizedException(
                    "Solo puedes registrar tu propio progreso");
        }

        // Cargamos el contenido para obtener el moduloId y el título.
        // Lanzamos 404 si no existe — no tiene sentido registrar progreso
        // sobre algo que no está en la plataforma.
        ContentItem contenido = contentRepository.findById(request.getContenidoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Contenido no encontrado con id: " + request.getContenidoId()));

        // Buscamos registro existente para hacer upsert y evitar duplicados
        UserProgress progreso = progressRepository
                .findByUsuarioIdAndContenidoId(
                        request.getUsuarioId(), request.getContenidoId())
                .orElse(UserProgress.builder()
                        .usuarioId(request.getUsuarioId())
                        .contenidoId(request.getContenidoId())
                        .moduloId(contenido.getModuloId())
                        .empresaId(request.getEmpresaId())
                        .build());

        // Acumulamos el tiempo de la sesión actual sobre el total histórico
        progreso.setTiempoSegundos(
                progreso.getTiempoSegundos() + request.getTiempoSegundos());

        // Actualizamos el porcentaje solo si el nuevo es mayor al que ya tenía —
        // así nunca retrocede aunque el frontend envíe un valor menor
        if (request.getPorcentajeCompletado() > progreso.getPorcentajeCompletado()) {
            progreso.setPorcentajeCompletado(request.getPorcentajeCompletado());
        }

        progreso.setVersionLeida(request.getVersionLeida());
        progreso.setHashAceptacion(request.getHashAceptacion());

        // Completado es irreversible — una vez marcado no se puede desmarcar
        if (request.isCompletado() && !progreso.isCompletado()) {
            progreso.setCompletado(true);
            progreso.setFechaCompletado(OffsetDateTime.now());
            progreso.setPorcentajeCompletado(100);
            log.info("Contenido {} completado por usuario {}",
                    request.getContenidoId(), request.getUsuarioId());

            // Notificación personalizada con el título real del contenido
            notificationService.crearInterna(
                    request.getUsuarioId(),
                    "CONTENIDO_COMPLETADO",
                    "¡Has completado \"" + contenido.getTitulo() + "\"! Sigue así.",
                    "/formacion/contenido/" + request.getContenidoId()
            );
        }

        return toResponse(progressRepository.save(progreso));
    }


    // ── PROGRESO PROPIO DEL EMPLEADO ──────────────────────────────────────

    /**
     * Devuelve todo el progreso del usuario autenticado ordenado por última actividad.
     * Cada registro incluye el moduloId para que el frontend pueda agrupar por módulo.
     */
    public List<ProgressResponse> miProgreso(User user) {
        return progressRepository
                .findByUsuarioIdOrderByActualizadoEnDesc(user.getUsuarioId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }


    // ── PROGRESO DE UN EMPLEADO CONCRETO (ADMIN) ──────────────────────────

    /**
     * Devuelve el progreso de un empleado concreto.
     * Admin empresa solo puede consultar empleados de su propia empresa.
     */
    public List<ProgressResponse> progresoPorUsuario(UUID usuarioId, User user) {
        String rol = user.getRol().getCodigoRol();

        if ("ROLE_ADMIN".equals(rol)) {
            return progressRepository
                    .findByUsuarioIdOrderByActualizadoEnDesc(usuarioId)
                    .stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }

        // Admin empresa solo ve empleados de su empresa — seguridad cross-company
        return progressRepository
                .findByUsuarioIdAndEmpresaId(usuarioId, user.getEmpresaId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }


    // ── PROGRESO DE TODA LA EMPRESA (DASHBOARD ADMIN) ─────────────────────

    /**
     * Devuelve todo el progreso de los empleados de una empresa.
     * Admin empresa solo puede consultar su propia empresa.
     */
    public List<ProgressResponse> progresoPorEmpresa(UUID empresaId, User user) {
        String rol = user.getRol().getCodigoRol();

        if ("ROLE_ADMIN_EMPRESA".equals(rol)
                && !user.getEmpresaId().equals(empresaId)) {
            throw new UnauthorizedException(
                    "Solo puedes consultar el progreso de tu empresa");
        }

        return progressRepository
                .findByEmpresaIdOrderByActualizadoEnDesc(empresaId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }


    // ── PROGRESO SOBRE UN CONTENIDO CONCRETO ─────────────────────────────

    /**
     * Devuelve el estado del usuario autenticado sobre un contenido concreto.
     * Si no hay registro previo devuelve estado PENDIENTE virtual sin persistir —
     * así el frontend puede mostrar el estado correcto aunque el empleado
     * no haya abierto el contenido todavía.
     */
    public ProgressResponse progresoPorContenido(UUID contenidoId, User user) {
        return progressRepository
                .findByUsuarioIdAndContenidoId(user.getUsuarioId(), contenidoId)
                .map(this::toResponse)
                .orElse(ProgressResponse.builder()
                        .usuarioId(user.getUsuarioId())
                        .contenidoId(contenidoId)
                        .empresaId(user.getEmpresaId())
                        .completado(false)
                        .tiempoSegundos(0)
                        .porcentajeCompletado(0)
                        .estado(ProgressStatus.PENDIENTE)
                        .build());
    }


    // ── DERIVAR ESTADO DESDE CAMPOS PERSISTIDOS ───────────────────────────

    /**
     * Calcula el estado de progreso a partir de los datos persistidos.
     * Este campo NO se guarda en BD — se construye en cada respuesta.
     *
     *   PENDIENTE   → nunca abierto (tiempoSegundos = 0 y no completado)
     *   EN_PROGRESO → ha empezado pero aún no lo ha marcado como completado
     *   COMPLETADO  → completado = true
     */
    private ProgressStatus derivarEstado(UserProgress p) {
        if (p.isCompletado()) return ProgressStatus.COMPLETADO;
        if (p.getTiempoSegundos() > 0) return ProgressStatus.EN_PROGRESO;
        return ProgressStatus.PENDIENTE;
    }


    // ── MAPPER INTERNO ────────────────────────────────────────────────────

    /**
     * Convierte una entidad UserProgress en su DTO de respuesta.
     * Incluye moduloId para que el frontend pueda agrupar por módulo
     * sin necesitar llamadas adicionales al backend.
     */
    private ProgressResponse toResponse(UserProgress p) {
        return ProgressResponse.builder()
                .registroId(p.getRegistroId())
                .usuarioId(p.getUsuarioId())
                .contenidoId(p.getContenidoId())
                .moduloId(p.getModuloId())
                .empresaId(p.getEmpresaId())
                .completado(p.isCompletado())
                .fechaCompletado(p.getFechaCompletado())
                .tiempoSegundos(p.getTiempoSegundos())
                .porcentajeCompletado(p.getPorcentajeCompletado())
                .versionLeida(p.getVersionLeida())
                .hashAceptacion(p.getHashAceptacion())
                .estado(derivarEstado(p))
                .actualizadoEn(p.getActualizadoEn())
                .build();
    }
}