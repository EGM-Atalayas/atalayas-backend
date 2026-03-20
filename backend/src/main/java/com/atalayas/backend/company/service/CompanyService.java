package com.atalayas.backend.company.service;

import com.atalayas.backend.common.enums.EstadoSolicitud;
import com.atalayas.backend.common.enums.RoleType;
import com.atalayas.backend.communication.service.EmailService;
import com.atalayas.backend.company.dto.CompanyResponse;
import com.atalayas.backend.company.dto.SolicitudAltaEmpresaRequest;
import com.atalayas.backend.company.dto.SolicitudAltaEmpresaResponse;
import com.atalayas.backend.company.entity.Company;
import com.atalayas.backend.company.mapper.CompanyMapper;
import com.atalayas.backend.company.repository.CompanyRepository;
import com.atalayas.backend.exception.BusinessException;
import com.atalayas.backend.exception.ResourceNotFoundException;
import com.atalayas.backend.role.entity.Rol;
import com.atalayas.backend.role.repository.RoleRepository;
import com.atalayas.backend.user.entity.User;
import com.atalayas.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * POST /empresas — solicitud de alta pública.
     * Crea la empresa en estado PENDIENTE y su usuario admin con activo=false.
     * Ambas entidades se persisten en la misma transacción; si falla alguna
     * operación se hace rollback completo.
     */
    @Transactional
    public SolicitudAltaEmpresaResponse crearEmpresa(SolicitudAltaEmpresaRequest request) {
        if (companyRepository.existsByCif(request.getCif())) {
            throw new BusinessException("Ya existe una empresa registrada con el CIF: " + request.getCif());
        }
        if (userRepository.existsByEmail(request.getEmailAdmin())) {
            throw new BusinessException("Ya existe un usuario registrado con el email: " + request.getEmailAdmin());
        }

        // 1 — Guardar empresa
        Company company = companyMapper.toEntityFromSolicitud(request);
        company = companyRepository.save(company);

        // 2 — Buscar el rol ADMIN_EMPRESA
        Rol rolAdminEmpresa = roleRepository.findByCodigoRol(RoleType.ROLE_ADMIN_EMPRESA.name())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rol no encontrado en BD: " + RoleType.ROLE_ADMIN_EMPRESA.name() +
                        ". Asegúrate de que existe la fila correspondiente en la tabla rol."));

        // 3 — Crear usuario inactivo vinculado a la empresa recién creada
        User adminUser = User.builder()
                .nombre(request.getNombre())
                .apellidos(request.getApellidos())
                .email(request.getEmailAdmin())
                .password(passwordEncoder.encode(request.getPassword()))
                .empresaId(company.getEmpresaId())
                .rol(rolAdminEmpresa)
                .activo(false)
                .build();
        adminUser = userRepository.save(adminUser);

        return companyMapper.toSolicitudResponse(company, adminUser);
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

    /**
     * PATCH /empresas/{id}/aprobar — aprueba la solicitud (SUPER_ADMIN).
     * Activa todos los usuarios inactivos de la empresa y envía email de bienvenida.
     */
    @Transactional
    public CompanyResponse aprobar(UUID id) {
        Company company = findOrThrow(id);
        if (company.getEstadoSolicitud() == EstadoSolicitud.APROBADA) {
            throw new BusinessException("La empresa ya está aprobada");
        }
        company.setEstadoSolicitud(EstadoSolicitud.APROBADA);
        company.setFechaResolucion(LocalDateTime.now());
        companyRepository.save(company);

        // Activar usuarios pendientes y notificar
        List<User> usuariosPendientes = userRepository.findAllByEmpresaIdAndActivoFalse(company.getEmpresaId());
        for (User u : usuariosPendientes) {
            u.setActivo(true);
            userRepository.save(u);
            emailService.enviarAprobacion(u.getEmail(), u.getNombre(), company.getNombreEmpresa());
        }

        return companyMapper.toResponse(company);
    }

    /**
     * PATCH /empresas/{id}/rechazar — rechaza la solicitud (SUPER_ADMIN).
     * El usuario queda en BD con activo=false; se envía email de notificación.
     */
    @Transactional
    public CompanyResponse rechazar(UUID id) {
        Company company = findOrThrow(id);
        if (company.getEstadoSolicitud() == EstadoSolicitud.RECHAZADA) {
            throw new BusinessException("La empresa ya está rechazada");
        }
        company.setEstadoSolicitud(EstadoSolicitud.RECHAZADA);
        company.setFechaResolucion(LocalDateTime.now());
        companyRepository.save(company);

        // Notificar a los usuarios inactivos de la empresa rechazada
        List<User> usuariosPendientes = userRepository.findAllByEmpresaIdAndActivoFalse(company.getEmpresaId());
        for (User u : usuariosPendientes) {
            emailService.enviarRechazo(u.getEmail(), u.getNombre(), company.getNombreEmpresa());
        }

        return companyMapper.toResponse(company);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Company findOrThrow(UUID id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada con id: " + id));
    }
}

