package com.atalayas.backend.content.service;

import com.atalayas.backend.common.enums.RoleType;
import com.atalayas.backend.content.dto.*;
import com.atalayas.backend.content.entity.ContentItem;
import com.atalayas.backend.content.entity.Question;
import com.atalayas.backend.content.mapper.ContentMapper;
import com.atalayas.backend.content.repository.ContentRepository;
import com.atalayas.backend.content.repository.QuestionRepository;
import com.atalayas.backend.exception.ResourceNotFoundException;
import com.atalayas.backend.exception.UnauthorizedException;
import com.atalayas.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * Lógica de negocio para contenidos formativos y sus preguntas de evaluación
 *
 * Reglas de acceso:
 *   - ROLE_ADMIN         - acceso total
 *   - ROLE_ADMIN_EMPRESA - gestiona contenidos de su empresa, ve globales
 *   - ROLE_EMPLEADO      - solo lectura de contenidos activos de su empresa + globales
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContentService {

    private final ContentRepository contentRepository;
    private final QuestionRepository questionRepository;
    private final ContentMapper contentMapper;


    // ── CRAER CONTENIDO ──────────────────────────────────────────────────────
    /**
     * Crea un nuevo contenido dentro de un módulo
     * Admin empresa siempre crea en su empresa - no puede crear contenido global
     */
    @Transactional
    public ContentResponse crear(ContentRequest request, User user) {
        RoleType rol = user.getRol().getRoleType();

        // Admin empresa no puede asignar contenido a otra empresa ni crear global
        UUID empresaId = (rol == RoleType.ROLE_ADMIN_EMPRESA)
                ? user.getEmpresaId()
                : request.getEmpresaId();

        ContentItem guardado = contentRepository.save(contentMapper.toEntity(request, empresaId));
        log.info("Contenido creado: {} en módulo: {} por: {}",
                guardado.getContenidoId(), guardado.getModuloId(), user.getEmail());

        // Las preguntas se añaden después via endpoint específico
        return contentMapper.toResponse(guardado, List.of());
    }


    // ── LISTAR POR MÓDULO ────────────────────────────────────────────────────
    /**
     * Devuelve los contenidos de un módulo según el rol:
     *   - Admin y admin empresa: todos (activos e inactivos) para gestionar
     *   - Empleado: solo los activos
     */
    public List<ContentResponse> listarPorModulo(UUID moduloId, User user) {
        RoleType rol = user.getRol().getRoleType();

        List<ContentItem> contenidos = (rol == RoleType.ROLE_EMPLEADO)
                ? contentRepository.findByModuloIdAndActivoTrueOrderByOrdenAsc(moduloId)
                : contentRepository.findByModuloIdOrderByOrdenAsc(moduloId);

        return contenidos.stream()
                .map(c -> contentMapper.toResponse(c,
                        questionRepository.findByContenidoIdOrderByPreguntaId(c.getContenidoId())))
                .collect(Collectors.toList());
    }


    // ── OBTENER POR ID ───────────────────────────────────────────────────────
    /**
     * Devuelve un contenido por ID con sus preguntas si es EVALUACION
     * Valida que el usuario tenga acceso al contenido
     */
    public ContentResponse obtenerPorId(UUID contenidoId, User user) {
        ContentItem contenido = contentRepository.findById(contenidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Contenido no encontrado: " + contenidoId));

        validarAccesoLectura(contenido, user);

        List<Question> preguntas = questionRepository.findByContenidoIdOrderByPreguntaId(contenidoId);
        return contentMapper.toResponse(contenido, preguntas);
    }


    // ── ACTUALIZAR CONTENIDO ─────────────────────────────────────────────────
    /**
     * Actualiza un contenido existente
     * Incrementa la versión automáticamente si cambia el cuerpo o la URL
     * para que la trazabilidad detecte empleados que necesitan releer
     */
    @Transactional
    public ContentResponse actualizar(UUID contenidoId, ContentRequest request, User user) {
        ContentItem contenido = contentRepository.findById(contenidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Contenido no encontrado: " + contenidoId));

        validarAccesoEscritura(contenido, user);

        // Si el cuerpo del contenido cambia, forzamos versión nueva
        boolean cuerpoModificado = !nullSafeEquals(contenido.getCuerpoTexto(), request.getCuerpoTexto())
                || !nullSafeEquals(contenido.getUrlRecurso(), request.getUrlRecurso());

        contenido.setTitulo(request.getTitulo());
        contenido.setDescripcion(request.getDescripcion());
        contenido.setTipoContenido(request.getTipoContenido());
        contenido.setUrlRecurso(request.getUrlRecurso());
        contenido.setCuerpoTexto(request.getCuerpoTexto());
        contenido.setOrden(request.getOrden());
        contenido.setMinutosEstimados(request.getMinutosEstimados());
        contenido.setEsIaGenerado(request.isEsIaGenerado());
        contenido.setActivo(request.isActivo());

        if (cuerpoModificado) {
            contenido.setVersion(contenido.getVersion() + 1);
            log.info("Contenido {} actualizado a versión: {}", contenidoId, contenido.getVersion());
        }

        List<Question> preguntas = questionRepository.findByContenidoIdOrderByPreguntaId(contenidoId);
        return contentMapper.toResponse(contentRepository.save(contenido), preguntas);
    }


    // ── DESACTIVAR CONTENIDO ─────────────────────────────────────────────────
    /**
     * Soft delete del contenido
     * La trazabilidad histórica en trazabilidad_lectura se conserva intacta
     * Lanza IllegalStateException si ya estaba desactivado - el GlobalExceptionHandler lo convierte en 400
     */
    @Transactional
    public ContentResponse desactivar(UUID contenidoId, User user) {
        ContentItem contenido = contentRepository.findById(contenidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Contenido no encontrado: " + contenidoId));

        validarAccesoEscritura(contenido, user);

        if (!contenido.isActivo()) {
            throw new IllegalStateException("El contenido ya está desactivado");
        }

        contenido.setActivo(false);
        log.info("Contenido desactivado: {} por: {}", contenidoId, user.getEmail());

        List<Question> preguntas = questionRepository.findByContenidoIdOrderByPreguntaId(contenidoId);
        return contentMapper.toResponse(contentRepository.save(contenido), preguntas);
    }


    // ── PREGUNTAS DE EVALUACIÓN ──────────────────────────────────────────────
    /**
     * Añade una pregunta a un contenido de tipo EVALUACION
     * Valida que el contenido padre existe y que el usuario tiene acceso de escritura
     */
    @Transactional
    public QuestionResponse crearPregunta(QuestionRequest request, User user) {
        ContentItem contenido = contentRepository.findById(request.getContenidoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Contenido no encontrado: " + request.getContenidoId()));

        validarAccesoEscritura(contenido, user);

        Question pregunta = Question.builder()
                .contenidoId(request.getContenidoId())
                .enunciado(request.getEnunciado())
                .respuestaCorrecta(request.getRespuestaCorrecta())
                .build();

        Question guardada = questionRepository.save(pregunta);
        log.info("Pregunta creada: {} en contenido: {}", guardada.getPreguntaId(), request.getContenidoId());

        return contentMapper.toQuestionResponse(guardada);
    }


    /**
     * Elimina una pregunta de evaluación por ID
     * Valida acceso de escritura sobre el contenido padre antes de eliminar
     */
    @Transactional
    public void eliminarPregunta(UUID preguntaId, User user) {
        Question pregunta = questionRepository.findById(preguntaId)
                .orElseThrow(() -> new ResourceNotFoundException("Pregunta no encontrada: " + preguntaId));

        ContentItem contenido = contentRepository.findById(pregunta.getContenidoId())
                .orElseThrow(() -> new ResourceNotFoundException("Contenido padre no encontrado"));

        validarAccesoEscritura(contenido, user);
        questionRepository.delete(pregunta);
        log.info("Pregunta eliminada: {} por: {}", preguntaId, user.getEmail());
    }


    // ── VALIDACIONES DE ACCESO ───────────────────────────────────────────────

    private void validarAccesoLectura(ContentItem contenido, User user) {
        RoleType rol = user.getRol().getRoleType();
        if (rol == RoleType.ROLE_ADMIN) return;

        boolean esGlobal = contenido.getEmpresaId() == null;
        boolean esDeSuEmpresa = user.getEmpresaId().equals(contenido.getEmpresaId());

        if (!esGlobal && !esDeSuEmpresa) {
            throw new UnauthorizedException("No tienes acceso a este contenido");
        }
    }

    private void validarAccesoEscritura(ContentItem contenido, User user) {
        RoleType rol = user.getRol().getRoleType();
        if (rol == RoleType.ROLE_ADMIN) return;

        if (contenido.getEmpresaId() == null) {
            throw new UnauthorizedException("Solo el superadmin puede modificar contenido global");
        }

        if (!user.getEmpresaId().equals(contenido.getEmpresaId())) {
            throw new UnauthorizedException("No puedes modificar contenido de otra empresa");
        }
    }


    // ── HELPER ───────────────────────────────────────────────────────────────
    /**
     * Comparación null-safe de dos strings
     * Necesaria para detectar cambios en campos opcionales como cuerpoTexto o urlRecurso
     * sin lanzar NullPointerException si alguno de los dos es null
     */
    private boolean nullSafeEquals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}