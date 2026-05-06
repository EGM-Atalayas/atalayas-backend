package com.atalayas.backend.servicios;

import com.atalayas.backend.exception.ResourceNotFoundException;
import com.atalayas.backend.servicios.dto.ServicioRequest;
import com.atalayas.backend.servicios.dto.ServicioResponse;
import com.atalayas.backend.servicios.entity.Servicio;
import com.atalayas.backend.servicios.enums.CategoriaServicio;
import com.atalayas.backend.servicios.mapper.ServicioMapper;
import com.atalayas.backend.servicios.repository.ServicioRepository;
import com.atalayas.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lógica de negocio para servicios del área empresarial.
 *
 * Reglas de acceso:
 *   - ROLE_ADMIN    → acceso total (crear, editar, desactivar)
 *   - Cualquier autenticado → lectura de servicios activos
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServicioService {

    private final ServicioRepository servicioRepository;
    private final ServicioMapper servicioMapper;


    // ── CREAR ────────────────────────────────────────────────────────────────

    @Transactional
    public ServicioResponse crear(ServicioRequest request, User user) {
        Servicio guardado = servicioRepository.save(
                servicioMapper.toEntity(request, user.getUsuarioId()));
        log.info("Servicio creado: {} por usuario: {}", guardado.getServicioId(), user.getEmail());
        return servicioMapper.toResponse(guardado);
    }


    // ── LISTAR ───────────────────────────────────────────────────────────────

    /**
     * Devuelve todos los servicios activos, opcionalmente filtrados por categoría.
     * Cualquier usuario autenticado puede consultarlos.
     */
    public List<ServicioResponse> listar(CategoriaServicio categoria) {
        List<Servicio> lista = (categoria != null)
                ? servicioRepository.findByActivoTrueAndCategoriaOrderByCreadoEnDesc(categoria)
                : servicioRepository.findByActivoTrueOrderByCategoriaAscCreadoEnDesc();

        return lista.stream()
                .map(servicioMapper::toResponse)
                .collect(Collectors.toList());
    }


    // ── ACTUALIZAR ───────────────────────────────────────────────────────────

    @Transactional
    public ServicioResponse actualizar(UUID servicioId, ServicioRequest request, User user) {
        Servicio servicio = findOrThrow(servicioId);

        servicio.setTitulo(request.getTitulo());
        servicio.setDescripcion(request.getDescripcion());
        servicio.setCategoria(request.getCategoria());
        servicio.setIconoUrl(request.getIconoUrl());
        servicio.setUrlInfo(request.getUrlInfo());
        servicio.setTelefono(request.getTelefono());
        servicio.setComoAcceder(request.getComoAcceder());

        log.info("Servicio actualizado: {} por usuario: {}", servicioId, user.getEmail());
        return servicioMapper.toResponse(servicioRepository.save(servicio));
    }


    // ── DESACTIVAR ───────────────────────────────────────────────────────────

    @Transactional
    public ServicioResponse desactivar(UUID servicioId, User user) {
        Servicio servicio = findOrThrow(servicioId);

        if (!servicio.isActivo()) {
            throw new IllegalStateException("El servicio ya está desactivado");
        }

        servicio.setActivo(false);
        log.info("Servicio desactivado: {} por usuario: {}", servicioId, user.getEmail());
        return servicioMapper.toResponse(servicioRepository.save(servicio));
    }


    // ── PRIVADO ──────────────────────────────────────────────────────────────

    private Servicio findOrThrow(UUID servicioId) {
        return servicioRepository.findById(servicioId)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado: " + servicioId));
    }
}
