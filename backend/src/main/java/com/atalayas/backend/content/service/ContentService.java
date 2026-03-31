package com.atalayas.backend.content.service;

import com.atalayas.backend.content.dto.*;
import com.atalayas.backend.content.entity.ContentItem;
import com.atalayas.backend.content.entity.Question;
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


    // ── CREAR CONTENIDO ──────────────────────────────────────────────────────
    /**
     * Crea un nuevo contenido dentro de un módulo
     * Admin empresa siempre crea en su empresa - no puede crear contenido global
     */
    @Transactional
    public ContentResponse crear(ContentRequest request, User user) {
        String rol = user.getRol().getCodigoRol();

        // Admin empresa no puede asignar contenido a otra empresa
        UUID empresaId = request.getEmpresaId();
        if ("ROLE_ADMIN_EMPRESA".equals(rol)) {
            empresaId = user.getEmpresaId();
        }

        ContentItem contenido = ContentItem.builder()
                .moduloId(request.getModuloId())
                .empresaId(empresaId)
                .titulo(request.getTitulo())
                .descripcion(request.getDescripcion())
                .tipoContenido(request.getTipoContenido())
                .urlRecurso(request.getUrlRecurso())
                .cuerpoTexto(request.getCuerpoTexto())
                .orden(request.getOrden())
                .version(request.getVersion())
                .minutosEstimados(request.getMinutosEstimados())
                .esIaGenerado(request.isEsIaGenerado())
                .activo(request.isActivo())
                .build();

        ContentItem guardado = contentRepository.save(contenido);
        log.info("Contenido creado: {} en módulo: {} por: {}",
                guardado.getContenidoId(), guardado.getModuloId(), user.getEmail());

        // Las preguntas se añaden después via endpoint específico de preguntas
        return toResponse(guardado, List.of());
    }


    // ── LISTAR POR MÓDULO ────────────────────────────────────────────────────
    /**
     * Devuelve los contenidos de un módulo según el rol:
     *   - Admin y admin empresa: ven todos (activos e inactivos) para gestionar
     *   - Empleado: solo ve los activos
     */
    public List<ContentResponse> listarPorModulo(UUID moduloId, User user) {
        String rol = user.getRol().getCodigoRol();

        List<ContentItem> contenidos;
        if ("ROLE_ADMIN".equals(rol) || "ROLE_ADMIN_EMPRESA".equals(rol)) {
            contenidos = contentRepository.findByModuloIdOrderByOrdenAsc(moduloId);
        } else {
            contenidos = contentRepository.findByModuloIdAndActivoTrueOrderByOrdenAsc(moduloId);
        }

        return contenidos.stream()
                .map(c -> toResponse(c, questionRepository.findByContenidoIdOrderByPreguntaId(c.getContenidoId())))
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
        return toResponse(contenido, preguntas);
    }


    // ── ACTUALIZAR CONTENIDO ─────────────────────────────────────────────────
    /**
     * Actualiza un contenido existente
     * Incrementa la versión automáticamente para invalidar trazabilidad anterior
     * si el cuerpo del contenido cambia
     */
    @Transactional
    public ContentResponse actualizar(UUID contenidoId, ContentRequest request, User user) {
        ContentItem contenido = contentRepository.findById(contenidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Contenido no encontrado: " + contenidoId));

        validarAccesoEscritura(contenido, user);

        // Si cambia el cuerpo del contenido, incrementamos versión automáticamente
        boolean cuerpoModificado = !equals(contenido.getCuerpoTexto(), request.getCuerpoTexto())
                || !equals(contenido.getUrlRecurso(), request.getUrlRecurso());

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
            log.info("Contenido {} actualizado con nueva versión: {}", contenidoId, contenido.getVersion());
        }

        List<Question> preguntas = questionRepository.findByContenidoIdOrderByPreguntaId(contenidoId);
        return toResponse(contentRepository.save(contenido), preguntas);
    }


    // ── DESACTIVAR CONTENIDO ─────────────────────────────────────────────────
    /**
     * Soft delete del contenido
     * La trazabilidad histórica se conserva intacta en trazabilidad_lectura
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
        return toResponse(contentRepository.save(contenido), preguntas);
    }


    // ── PREGUNTAS DE EVALUACIÓN ──────────────────────────────────────────────
    /**
     * Añade una pregunta a un contenido de tipo EVALUACION
     * Solo admins pueden gestionar preguntas
     */
    @Transactional
    public QuestionResponse crearPregunta(QuestionRequest request, User user) {
        // Verificamos que el contenido existe antes de crear la pregunta
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

        return toQuestionResponse(guardada);
    }


    /**
     * Elimina una pregunta de evaluación por ID
     */
    @Transactional
    public void eliminarPregunta(UUID preguntaId, User user) {
        Question pregunta = questionRepository.findById(preguntaId)
                .orElseThrow(() -> new ResourceNotFoundException("Pregunta no encontrada: " + preguntaId));

        // Validamos acceso sobre el contenido padre
        ContentItem contenido = contentRepository.findById(pregunta.getContenidoId())
                .orElseThrow(() -> new ResourceNotFoundException("Contenido no encontrado"));

        validarAccesoEscritura(contenido, user);
        questionRepository.delete(pregunta);
        log.info("Pregunta eliminada: {} por: {}", preguntaId, user.getEmail());
    }


    // ── VALIDACIONES DE ACCESO ───────────────────────────────────────────────

    private void validarAccesoLectura(ContentItem contenido, User user) {
        String rol = user.getRol().getCodigoRol();
        if ("ROLE_ADMIN".equals(rol)) return;

        boolean esGlobal = contenido.getEmpresaId() == null;
        boolean esDeSuEmpresa = user.getEmpresaId().equals(contenido.getEmpresaId());

        if (!esGlobal && !esDeSuEmpresa) {
            throw new UnauthorizedException("No tienes acceso a este contenido");
        }
    }

    private void validarAccesoEscritura(ContentItem contenido, User user) {
        String rol = user.getRol().getCodigoRol();
        if ("ROLE_ADMIN".equals(rol)) return;

        if (contenido.getEmpresaId() == null) {
            throw new UnauthorizedException("Solo el superadmin puede modificar contenido global");
        }

        if (!user.getEmpresaId().equals(contenido.getEmpresaId())) {
            throw new UnauthorizedException("No puedes modificar contenido de otra empresa");
        }
    }


    // ── HELPERS ──────────────────────────────────────────────────────────────

    // Comparación null-safe para detectar cambios en el contenido
    private boolean equals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }


    // ── MAPPERS INTERNOS ─────────────────────────────────────────────────────

    private ContentResponse toResponse(ContentItem c, List<Question> preguntas) {
        return ContentResponse.builder()
                .contenidoId(c.getContenidoId())
                .moduloId(c.getModuloId())
                .empresaId(c.getEmpresaId())
                .titulo(c.getTitulo())
                .descripcion(c.getDescripcion())
                .tipoContenido(c.getTipoContenido())
                .urlRecurso(c.getUrlRecurso())
                .cuerpoTexto(c.getCuerpoTexto())
                .orden(c.getOrden())
                .version(c.getVersion())
                .minutosEstimados(c.getMinutosEstimados())
                .esIaGenerado(c.isEsIaGenerado())
                .activo(c.isActivo())
                .preguntas(preguntas.stream().map(this::toQuestionResponse).collect(Collectors.toList()))
                .fechaCreacion(c.getFechaCreacion())
                .actualizadoEn(c.getActualizadoEn())
                .build();
    }

    private QuestionResponse toQuestionResponse(Question q) {
        return QuestionResponse.builder()
                .preguntaId(q.getPreguntaId())
                .contenidoId(q.getContenidoId())
                .enunciado(q.getEnunciado())
                .respuestaCorrecta(q.getRespuestaCorrecta())
                .actualizadoEn(q.getActualizadoEn())
                .build();
    }
}