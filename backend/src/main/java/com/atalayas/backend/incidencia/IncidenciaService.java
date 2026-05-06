package com.atalayas.backend.incidencia;

import com.atalayas.backend.audit.service.AuditService;
import com.atalayas.backend.exception.ResourceNotFoundException;
import com.atalayas.backend.incidencia.dto.IncidenciaRequest;
import com.atalayas.backend.incidencia.dto.IncidenciaResponse;
import com.atalayas.backend.incidencia.entity.Incidencia;
import com.atalayas.backend.incidencia.enums.EstadoIncidencia;
import com.atalayas.backend.incidencia.enums.PrioridadIncidencia;
import com.atalayas.backend.incidencia.mapper.IncidenciaMapper;
import com.atalayas.backend.incidencia.repository.IncidenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IncidenciaService {

    private final IncidenciaRepository incidenciaRepository;
    private final IncidenciaMapper incidenciaMapper;
    private final AuditService auditService;

    @Transactional
    public IncidenciaResponse crear(IncidenciaRequest request) {
        Incidencia incidencia = incidenciaMapper.toEntity(request);
        Incidencia saved = incidenciaRepository.save(incidencia);

        if (PrioridadIncidencia.CRITICA.equals(saved.getPrioridad())) {
            auditService.registrar(
                    "Incidencia crítica abierta: " + saved.getTitulo(), "error");
        }

        return incidenciaMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<IncidenciaResponse> listarTodas() {
        return incidenciaRepository.findAllByOrderByCreadoEnDesc()
                .stream()
                .map(incidenciaMapper::toResponse)
                .toList();
    }

    @Transactional
    public IncidenciaResponse cerrar(Long id) {
        Incidencia incidencia = incidenciaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Incidencia no encontrada con id: " + id));
        incidencia.setEstado(EstadoIncidencia.CERRADA);
        return incidenciaMapper.toResponse(incidenciaRepository.save(incidencia));
    }

    @Transactional
    public IncidenciaResponse cambiarEstado(Long id, EstadoIncidencia nuevoEstado) {
        Incidencia incidencia = incidenciaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Incidencia no encontrada con id: " + id));
        incidencia.setEstado(nuevoEstado);
        return incidenciaMapper.toResponse(incidenciaRepository.save(incidencia));
    }
}

