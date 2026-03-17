package com.atalayas.backend.company.service;

import com.atalayas.backend.common.enums.EstadoSolicitud;
import com.atalayas.backend.company.dto.CompanyRequest;
import com.atalayas.backend.company.dto.CompanyResponse;
import com.atalayas.backend.company.entity.Company;
import com.atalayas.backend.company.mapper.CompanyMapper;
import com.atalayas.backend.company.repository.CompanyRepository;
import com.atalayas.backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;

    /** POST /empresas — solicitud de alta, abierta sin autenticación. */
    @Transactional
    public CompanyResponse crearEmpresa(CompanyRequest request) {
        if (companyRepository.existsByCif(request.getCif())) {
            throw new IllegalArgumentException("Ya existe una empresa registrada con el CIF: " + request.getCif());
        }
        Company company = companyMapper.toEntity(request);
        return companyMapper.toResponse(companyRepository.save(company));
    }

    /** GET /empresas — todas las empresas (SUPER_ADMIN). */
    @Transactional(readOnly = true)
    public List<CompanyResponse> getAll() {
        return companyRepository.findAll().stream()
                .map(companyMapper::toResponse)
                .collect(Collectors.toList());
    }

    /** GET /empresas/pendientes — solo las pendientes de resolución (SUPER_ADMIN). */
    @Transactional(readOnly = true)
    public List<CompanyResponse> getPendientes() {
        return companyRepository.findAllByEstadoSolicitud(EstadoSolicitud.PENDIENTE).stream()
                .map(companyMapper::toResponse)
                .collect(Collectors.toList());
    }

    /** GET /empresas/aprobadas — aprobadas y activas (endpoint público para selector de registro). */
    @Transactional(readOnly = true)
    public List<CompanyResponse> getAprobadas() {
        return companyRepository.findAllByEstadoSolicitudAndActivoTrue(EstadoSolicitud.APROBADA).stream()
                .map(companyMapper::toResponse)
                .collect(Collectors.toList());
    }

    /** PATCH /empresas/{id}/aprobar — aprueba la solicitud (SUPER_ADMIN). */
    @Transactional
    public CompanyResponse aprobar(UUID id) {
        Company company = findOrThrow(id);
        if (company.getEstadoSolicitud() == EstadoSolicitud.APROBADA) {
            throw new IllegalArgumentException("La empresa ya está aprobada");
        }
        company.setEstadoSolicitud(EstadoSolicitud.APROBADA);
        company.setFechaResolucion(LocalDateTime.now());
        return companyMapper.toResponse(companyRepository.save(company));
    }

    /** PATCH /empresas/{id}/rechazar — rechaza la solicitud (SUPER_ADMIN). */
    @Transactional
    public CompanyResponse rechazar(UUID id) {
        Company company = findOrThrow(id);
        if (company.getEstadoSolicitud() == EstadoSolicitud.RECHAZADA) {
            throw new IllegalArgumentException("La empresa ya está rechazada");
        }
        company.setEstadoSolicitud(EstadoSolicitud.RECHAZADA);
        company.setFechaResolucion(LocalDateTime.now());
        return companyMapper.toResponse(companyRepository.save(company));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Company findOrThrow(UUID id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada con id: " + id));
    }
}

