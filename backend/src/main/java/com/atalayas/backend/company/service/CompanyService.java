package com.atalayas.backend.company.service;

import com.atalayas.backend.audit.service.AuditService;
import com.atalayas.backend.common.enums.EstadoSolicitud;
import com.atalayas.backend.common.enums.RoleType;
import com.atalayas.backend.communication.service.EmailService;
import com.atalayas.backend.communication.service.NotificationService;
import com.atalayas.backend.company.dto.AccionSolicitudRequest;
import com.atalayas.backend.company.dto.CambioEstadoRequest;
import com.atalayas.backend.company.dto.CompanyResponse;
import com.atalayas.backend.company.dto.SolicitudAltaEmpresaRequest;
import com.atalayas.backend.company.dto.SolicitudAltaEmpresaResponse;
import com.atalayas.backend.company.dto.SolicitudPendienteResponse;
import com.atalayas.backend.company.entity.Company;
import com.atalayas.backend.company.mapper.CompanyMapper;
import com.atalayas.backend.company.repository.CompanyRepository;
import com.atalayas.backend.exception.BusinessException;
import com.atalayas.backend.exception.ResourceNotFoundException;
import com.atalayas.backend.role.entity.Role;
import com.atalayas.backend.role.repository.RoleRepository;
import com.atalayas.backend.user.entity.User;
import com.atalayas.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lógica de negocio para la gestión de empresas del parque.
 *
 * Cubre el ciclo completo de una empresa en la plataforma:
 * solicitud de alta → revisión por EGM → aprobación o rechazo → gestión activa.
 *
 * Solo ROLE_ADMIN puede aprobar, rechazar y gestionar empresas.
 * El alta pública (formulario de solicitud) no requiere autenticación.
 */
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final AuditService auditService;


    // ── SOLICITUD DE ALTA ─────────────────────────────────────────────────

    /**
     * Procesa una solicitud pública de alta de empresa.
     *
     * Crea la empresa en estado PENDIENTE y su usuario admin con activo=false.
     * Ambas entidades se persisten en la misma transacción — si falla alguna
     * operación se hace rollback completo de las dos.
     */
    @Transactional
    public SolicitudAltaEmpresaResponse crearEmpresa(SolicitudAltaEmpresaRequest request) {
        if (companyRepository.existsByCif(request.getCif())) {
            throw new BusinessException(
                    "Ya existe una empresa registrada con el CIF: " + request.getCif());
        }
        if (userRepository.existsByEmail(request.getEmailAdmin())) {
            throw new BusinessException(
                    "Ya existe un usuario registrado con el email: " + request.getEmailAdmin());
        }

        // Paso 1 — Guardar la empresa en estado PENDIENTE
        Company company = companyMapper.toEntityFromSolicitud(request);
        company = companyRepository.save(company);

        // Paso 2 — Buscar el rol ADMIN_EMPRESA en BD
        // Si no existe es un error de configuración del sistema, no del usuario
        Role roleAdminEmpresa = roleRepository
                .findByCodigoRol(RoleType.ROLE_ADMIN_EMPRESA.name())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rol ROLE_ADMIN_EMPRESA no encontrado en BD. " +
                                "Asegúrate de que existe la fila en la tabla rol."));

        // Paso 3 — Crear el usuario admin inactivo vinculado a la empresa
        // Quedará inactivo hasta que EGM apruebe la solicitud
        User adminUser = User.builder()
                .nombre(request.getNombre())
                .apellidos(request.getApellidos())
                .email(request.getEmailAdmin())
                .password(passwordEncoder.encode(request.getPassword()))
                .empresaId(company.getEmpresaId())
                .rol(roleAdminEmpresa)
                .activo(false)
                .build();
        adminUser = userRepository.save(adminUser);

        return companyMapper.toSolicitudResponse(company, adminUser);
    }


    // ── CONSULTAS ─────────────────────────────────────────────────────────

    /** Devuelve todas las empresas — solo ROLE_ADMIN */
    @Transactional(readOnly = true)
    public List<CompanyResponse> getAll() {
        return companyRepository.findAll().stream()
                .map(companyMapper::toResponse)
                .collect(Collectors.toList());
    }

    /** Devuelve solo las empresas pendientes de resolución — solo ROLE_ADMIN */
    @Transactional(readOnly = true)
    public List<CompanyResponse> getPendientes() {
        return companyRepository
                .findAllByEstadoSolicitud(EstadoSolicitud.PENDIENTE).stream()
                .map(companyMapper::toResponse)
                .collect(Collectors.toList());
    }

    /** Devuelve las empresas aprobadas y activas — para el selector de registro */
    @Transactional(readOnly = true)
    public List<CompanyResponse> getAprobadas() {
        return companyRepository
                .findAllByEstadoSolicitudAndActivoTrue(EstadoSolicitud.APROBADA).stream()
                .map(companyMapper::toResponse)
                .collect(Collectors.toList());
    }


    // ── RESOLUCIÓN DE SOLICITUDES ─────────────────────────────────────────

    /**
     * Cambia el estado de una empresa — solo ROLE_ADMIN.
     *
     * Transiciones permitidas:
     *   PENDIENTE  → APROBADA  : activa empresa y usuarios, envía emails y notificaciones
     *   PENDIENTE  → RECHAZADA : desactiva empresa, envía email de rechazo
     *   RECHAZADA  → PENDIENTE : reset sin efectos secundarios
     *   APROBADA   → cualquier : PROHIBIDO — empresa ya operativa
     *   RECHAZADA  → APROBADA  : PROHIBIDO — debe pasar primero por PENDIENTE
     *   mismo      → mismo     : PROHIBIDO — cambio sin efecto
     */
    @Transactional
    public CompanyResponse cambiarEstado(UUID id, CambioEstadoRequest request) {
        Company company = findOrThrow(id);
        EstadoSolicitud actual = company.getEstadoSolicitud();
        EstadoSolicitud destino = request.getNuevoEstado();

        // Validamos que la transición sea permitida antes de tocar nada
        if (actual == destino) {
            throw new BusinessException("La empresa ya se encuentra en estado " + actual);
        }
        if (actual == EstadoSolicitud.APROBADA) {
            throw new BusinessException("Una empresa aprobada no puede cambiar de estado");
        }
        if (actual == EstadoSolicitud.RECHAZADA && destino == EstadoSolicitud.APROBADA) {
            throw new BusinessException(
                    "No se puede aprobar directamente una empresa rechazada. " +
                            "Primero debe volver al estado PENDIENTE.");
        }

        company.setEstadoSolicitud(destino);

        switch (destino) {

            case APROBADA -> {
                // Activamos la empresa y todos sus usuarios inactivos
                company.setActivo(true);
                company.setFechaResolucion(OffsetDateTime.now());
                companyRepository.save(company);

                List<User> inactivos = userRepository
                        .findAllByEmpresaIdAndActivoFalse(company.getEmpresaId());

                for (User u : inactivos) {
                    u.setActivo(true);
                    userRepository.save(u);

                    // Email de bienvenida + notificación interna para cada usuario activado
                    emailService.enviarAprobacion(
                            u.getEmail(), u.getNombre(), company.getNombreEmpresa());
                    notificationService.crearInterna(
                            u.getUsuarioId(),
                            "BIENVENIDA",
                            "¡Bienvenido/a a Atalayas, " + u.getNombre() + "! " +
                                    "Tu empresa \"" + company.getNombreEmpresa() +
                                    "\" ha sido activada. Empieza tu formación.",
                            "/dashboard"
                    );
                }
            }

            case RECHAZADA -> {
                // Desactivamos la empresa y notificamos por email al admin
                company.setActivo(false);
                company.setFechaResolucion(OffsetDateTime.now());
                companyRepository.save(company);

                List<User> inactivos = userRepository
                        .findAllByEmpresaIdAndActivoFalse(company.getEmpresaId());
                for (User u : inactivos) {
                    emailService.enviarRechazo(
                            u.getEmail(), u.getNombre(), company.getNombreEmpresa());
                }
            }

            case PENDIENTE -> {
                // Reset a estado inicial — empresa inactiva, sin fecha de resolución
                // Los usuarios siguen inactivos, no se envían emails
                company.setActivo(false);
                company.setFechaResolucion(null);
                companyRepository.save(company);
            }
        }

        return companyMapper.toResponse(company);
    }


    // ── SOLICITUDES (nuevo frontend superadmin) ──────────────────────────────

    /**
     * GET /api/v1/empresas/solicitudes
     * Lista todas las empresas en estado PENDIENTE con datos de su admin provisional.
     */
    @Transactional(readOnly = true)
    public List<SolicitudPendienteResponse> getSolicitudesPendientes() {
        return companyRepository.findAllByEstadoSolicitud(EstadoSolicitud.PENDIENTE).stream()
                .map(empresa -> {
                    User admin = userRepository
                            .findAllByEmpresaId(empresa.getEmpresaId()).stream()
                            .findFirst()
                            .orElse(null);
                    return companyMapper.toSolicitudPendienteResponse(empresa, admin);
                })
                .collect(Collectors.toList());
    }

    /**
     * PATCH /api/v1/empresas/{id}/solicitud
     * Aprueba o rechaza una solicitud mediante el campo {@code accion}: "aprobar" | "rechazar".
     * Delega en {@link #cambiarEstado} reutilizando toda la lógica existente
     * (emails, notificaciones, auditoría).
     */
    @Transactional
    public void resolverSolicitud(UUID id, AccionSolicitudRequest request) {
        EstadoSolicitud destino = "aprobar".equalsIgnoreCase(request.getAccion())
                ? EstadoSolicitud.APROBADA
                : EstadoSolicitud.RECHAZADA;

        CambioEstadoRequest cambio = new CambioEstadoRequest();
        cambio.setNuevoEstado(destino);
        cambiarEstado(id, cambio);

        // Traza de auditoría
        Company empresa = findOrThrow(id);
        String texto = "aprobar".equalsIgnoreCase(request.getAccion())
                ? "Empresa \"" + empresa.getNombreEmpresa() + "\" aprobada"
                : "Solicitud de \"" + empresa.getNombreEmpresa() + "\" rechazada";
        auditService.registrar(texto,
                "aprobar".equalsIgnoreCase(request.getAccion()) ? "success" : "warning");
    }


    // ── HELPER ───────────────────────────────────────────────────────────────

    private Company findOrThrow(UUID id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Empresa no encontrada con id: " + id));
    }
}