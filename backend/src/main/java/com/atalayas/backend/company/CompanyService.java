package com.atalayas.backend.company;

import com.atalayas.backend.audit.service.AuditService;
import com.atalayas.backend.common.dto.PaginatedResponse;
import com.atalayas.backend.common.enums.EstadoSolicitud;
import com.atalayas.backend.common.enums.RoleType;
import com.atalayas.backend.communication.service.EmailService;
import com.atalayas.backend.communication.service.NotificationService;
import com.atalayas.backend.company.CompanySpecifications;
import com.atalayas.backend.company.dto.AccionSolicitudRequest;
import com.atalayas.backend.company.dto.CambioEstadoRequest;
import com.atalayas.backend.company.dto.CompanyResponse;
import com.atalayas.backend.company.dto.ReenviarEmailRequest;
import com.atalayas.backend.company.dto.SolicitudAltaEmpresaRequest;
import com.atalayas.backend.company.dto.SolicitudAltaEmpresaResponse;
import com.atalayas.backend.company.dto.SolicitudPendienteResponse;
import com.atalayas.backend.company.entity.Company;
import com.atalayas.backend.company.event.CompanyEvent;
import com.atalayas.backend.company.mapper.CompanyMapper;
import com.atalayas.backend.company.repository.CompanyRepository;
import com.atalayas.backend.exception.BusinessException;
import com.atalayas.backend.exception.ResourceNotFoundException;
import com.atalayas.backend.role.entity.Rol;
import com.atalayas.backend.role.repository.RoleRepository;
import com.atalayas.backend.usuario.entity.User;
import com.atalayas.backend.usuario.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
@Slf4j
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
    private final ApplicationEventPublisher eventPublisher;


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
        Rol roleAdminEmpresa = roleRepository
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

        // Paso 4 — Notificar a todos los superadmins de la nueva solicitud
        final Company savedCompany = company;
        userRepository.findAllByRolCodigoRol("ROLE_ADMIN").forEach(admin ->
                notificationService.crearInterna(
                        admin.getUsuarioId(),
                        "SOLICITUD_EMPRESA",
                        "Nueva solicitud de registro: " + savedCompany.getNombreEmpresa(),
                        "/superadmin/solicitudes"
                )
        );

        return companyMapper.toSolicitudResponse(company, adminUser);
    }


    // ── CONSULTAS ─────────────────────────────────────────────────────────

    /** Devuelve todas las empresas — solo ROLE_ADMIN */
    @Transactional(readOnly = true)
    public List<CompanyResponse> getAll() {
        return companyRepository.findAll().stream()
                .map(empresa -> {
                    User admin = userRepository
                            .findAllByEmpresaId(empresa.getEmpresaId()).stream()
                            .findFirst()
                            .orElse(null);
                    return companyMapper.toResponse(empresa, admin);
                })
                .collect(Collectors.toList());
    }

    /** Devuelve empresas paginadas con filtros opcionales — solo ROLE_ADMIN */
    @Transactional(readOnly = true)
    public PaginatedResponse<CompanyResponse> getAllPaged(int page, int size, String search, EstadoSolicitud estado) {
        var spec = CompanySpecifications.filtered(search, estado);
        var pageResult = companyRepository.findAll(spec,
                PageRequest.of(page, size, Sort.by("nombreEmpresa").ascending()));
        return PaginatedResponse.of(pageResult, empresa -> {
            User admin = userRepository.findAllByEmpresaId(empresa.getEmpresaId())
                    .stream().findFirst().orElse(null);
            return companyMapper.toResponse(empresa, admin);
        });
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
     *   PENDIENTE → APROBADA  : activa empresa y usuarios, publica CompanyEvent → email tras commit
     *   APROBADA  → PAUSADA   : desactiva empresa y usuarios temporalmente
     *   PAUSADA   → APROBADA  : reactiva empresa y usuarios
     */
    @Transactional
    public CompanyResponse cambiarEstado(UUID id, CambioEstadoRequest request) {
        Company company = findOrThrow(id);
        EstadoSolicitud actual = company.getEstadoSolicitud();
        EstadoSolicitud destino = request.getNuevoEstado();

        if (actual == destino) {
            throw new BusinessException("La empresa ya se encuentra en estado " + actual);
        }

        // Transiciones permitidas
        boolean transicionValida = switch (actual) {
            case PENDIENTE -> destino == EstadoSolicitud.APROBADA;
            case APROBADA  -> destino == EstadoSolicitud.PAUSADA;
            case PAUSADA   -> destino == EstadoSolicitud.APROBADA;
            // RECHAZADA es un estado terminal gestionado por resolverSolicitud (hard delete)
            // No se permite ninguna transición desde cambiarEstado
            case RECHAZADA -> false;
        };

        if (!transicionValida) {
            throw new BusinessException(
                    "Transición no permitida: " + actual + " → " + destino +
                    ". El rechazo de solicitudes solo es posible desde PATCH /{id}/solicitud.");
        }

        company.setEstadoSolicitud(destino);

        switch (destino) {

            case APROBADA -> {
                company.setActivo(true);
                company.setFechaResolucion(OffsetDateTime.now());
                companyRepository.save(company);

                // En ambos casos (desde PENDIENTE o desde PAUSADA) los usuarios
                // están inactivos — se reactivan todos
                List<User> afectados = userRepository
                        .findAllByEmpresaIdAndActivoFalse(company.getEmpresaId());

                for (User u : afectados) {
                    u.setActivo(true);
                    userRepository.save(u);

                    if (actual == EstadoSolicitud.PENDIENTE) {
                        // Email enviado DESPUÉS del commit vía CompanyEventListener (@Async + AFTER_COMMIT)
                        // → un fallo SMTP nunca revierte el cambio de estado en BD
                        eventPublisher.publishEvent(new CompanyEvent(
                                company.getEmpresaId(),
                                company.getNombreEmpresa(),
                                u.getEmail(),
                                u.getNombre(),
                                actual,
                                destino
                        ));

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
            }

            case PAUSADA -> {
                // Suspensión temporal — desactivar empresa y todos sus usuarios
                company.setActivo(false);
                companyRepository.save(company);

                List<User> activos = userRepository
                        .findAllByEmpresaIdAndActivoTrue(company.getEmpresaId());
                for (User u : activos) {
                    u.setActivo(false);
                    userRepository.save(u);
                }
            }

            default -> throw new BusinessException("Estado de destino no gestionado: " + destino);
        }

        return companyMapper.toResponse(company);
    }


    // ── SOLICITUDES ──────────────────────────────────────────────────────────

    /**
     * Activa o desactiva una empresa APROBADA (toggle de activo).
     * No cambia estadoSolicitud. Activa/desactiva también todos sus usuarios.
     */
    @Transactional
    public CompanyResponse toggleActivacion(UUID id) {
        Company company = findOrThrow(id);

        if (company.getEstadoSolicitud() != EstadoSolicitud.APROBADA) {
            throw new BusinessException("Solo se puede activar/desactivar una empresa aprobada");
        }

        boolean nuevoActivo = !company.isActivo();
        company.setActivo(nuevoActivo);
        companyRepository.save(company);

        List<User> usuarios = userRepository.findAllByEmpresaId(company.getEmpresaId());
        for (User u : usuarios) {
            u.setActivo(nuevoActivo);
            userRepository.save(u);
        }

        return companyMapper.toResponse(company);
    }

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
     * Aprueba o rechaza una solicitud (accion: "aprobar" | "rechazar").
     *
     * - "aprobar": delega en cambiarEstado → publica CompanyEvent → email tras commit.
     * - "rechazar": captura datos del usuario ANTES del hard delete, publica CompanyEvent
     *               con estadoNuevo=RECHAZADA → email tras commit → hard delete.
     */
    @Transactional
    public void resolverSolicitud(UUID id, AccionSolicitudRequest request) {
        boolean aprobar = "aprobar".equalsIgnoreCase(request.getAccion());

        if (aprobar) {
            Company empresa = findOrThrow(id);
            String nombreEmpresa = empresa.getNombreEmpresa();

            CambioEstadoRequest cambio = new CambioEstadoRequest();
            cambio.setNuevoEstado(EstadoSolicitud.APROBADA);
            cambiarEstado(id, cambio);

            auditService.registrar(
                    "Empresa \"" + nombreEmpresa + "\" aprobada", "success");

        } else {
            Company empresa = findOrThrow(id);

            if (empresa.getEstadoSolicitud() != EstadoSolicitud.PENDIENTE) {
                throw new BusinessException(
                        "Solo se pueden rechazar empresas en estado PENDIENTE");
            }

            // Capturar datos ANTES del delete y publicar evento.
            // CompanyEventListener envía el email DESPUÉS del commit (AFTER_COMMIT + @Async),
            // cuando la empresa ya no existe en BD — los datos viajan en el record inmutable.
            List<User> usuarios = userRepository.findAllByEmpresaIdAndActivoFalse(empresa.getEmpresaId());
            for (User u : usuarios) {
                eventPublisher.publishEvent(new CompanyEvent(
                        empresa.getEmpresaId(),
                        empresa.getNombreEmpresa(),
                        u.getEmail(),
                        u.getNombre(),
                        EstadoSolicitud.PENDIENTE,
                        EstadoSolicitud.RECHAZADA
                ));
            }

            String nombreEmpresa = empresa.getNombreEmpresa();

            userRepository.deleteAllByEmpresaId(empresa.getEmpresaId());
            companyRepository.delete(empresa);

            auditService.registrar(
                    "Solicitud de \"" + nombreEmpresa + "\" rechazada y eliminada", "warning");
        }
    }

    /**
     * POST /api/v1/empresas/{id}/reenviar-email
     * Permite al superadmin reenviar el email de aprobación cuando emailEnviado=false.
     * Lanza EmailSendException si el envío falla — el GlobalExceptionHandler devuelve 502.
     */
    @Transactional
    public void reenviarEmail(UUID id, ReenviarEmailRequest request) {
        Company empresa = findOrThrow(id);

        if (empresa.getEstadoSolicitud() != EstadoSolicitud.APROBADA) {
            throw new BusinessException(
                    "Solo se puede reenviar email a empresas en estado APROBADA");
        }

        User admin = userRepository.findAllByEmpresaId(empresa.getEmpresaId()).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró usuario admin para la empresa: " + id));

        // EmailSendException se propaga sin capturar → GlobalExceptionHandler devuelve 502
        emailService.enviarAprobacion(
                admin.getEmail(), admin.getNombre(), empresa.getNombreEmpresa());

        empresa.setEmailEnviado(true);
        companyRepository.save(empresa);

        auditService.registrar(
                "Email de aprobación reenviado a empresa \"" + empresa.getNombreEmpresa() + "\"",
                "info");
    }


    // ── HELPER ───────────────────────────────────────────────────────────────

    private Company findOrThrow(UUID id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Empresa no encontrada con id: " + id));
    }
}

