package com.atalayas.backend.company;

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
import com.atalayas.backend.role.entity.Rol;
import com.atalayas.backend.role.repository.RoleRepository;
import com.atalayas.backend.user.entity.User;
import com.atalayas.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
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
     * Transiciones permitidas desde este endpoint (PATCH /{id}/estado):
     *   PENDIENTE → APROBADA  : activa empresa y usuarios, envía emails y notificaciones
     *   APROBADA  → PAUSADA   : desactiva empresa y usuarios temporalmente
     *   PAUSADA   → APROBADA  : reactiva empresa y usuarios
     *
     * El rechazo (con hard delete) solo se puede realizar desde PATCH /{id}/solicitud.
     * Cualquier otra transición devuelve 400 Bad Request.
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
                        // Primera aprobación: email de bienvenida + notificación interna.
                        // El email está intencionalmente fuera del contrato transaccional:
                        // un fallo SMTP no debe revertir el cambio de estado en BD.
                        // Ver docs/email-transactional-pattern.md para la alternativa
                        // robusta basada en @TransactionalEventListener.
                        try {
                            emailService.enviarAprobacion(
                                    u.getEmail(), u.getNombre(), company.getNombreEmpresa());
                        } catch (MailException ex) {
                            log.warn("No se pudo enviar email de aprobación a {} — empresa={}: {}",
                                    u.getEmail(), company.getNombreEmpresa(), ex.getMessage());
                        }
                        notificationService.crearInterna(
                                u.getUsuarioId(),
                                "BIENVENIDA",
                                "¡Bienvenido/a a Atalayas, " + u.getNombre() + "! " +
                                        "Tu empresa \"" + company.getNombreEmpresa() +
                                        "\" ha sido activada. Empieza tu formación.",
                                "/dashboard"
                        );
                    }
                    // Reactivación desde PAUSADA: sin email, solo se reactiva el acceso
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


    // ── SOLICITUDES (nuevo frontend superadmin) ──────────────────────────────

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
     * Aprueba o rechaza una solicitud mediante el campo {@code accion}: "aprobar" | "rechazar".
     *
     * - "aprobar": delega en {@link #cambiarEstado} (activa empresa y usuarios, envía emails).
     * - "rechazar": envía email de rechazo y elimina físicamente usuarios y empresa de la BD
     *               (en ese orden para respetar la FK usuario → empresa).
     */
    @Transactional
    public void resolverSolicitud(UUID id, AccionSolicitudRequest request) {
        boolean aprobar = "aprobar".equalsIgnoreCase(request.getAccion());

        if (aprobar) {
            // Capturar nombre antes de cambiarEstado para tenerlo disponible en el audit
            Company empresa = findOrThrow(id);
            String nombreEmpresa = empresa.getNombreEmpresa();

            CambioEstadoRequest cambio = new CambioEstadoRequest();
            cambio.setNuevoEstado(EstadoSolicitud.APROBADA);
            cambiarEstado(id, cambio);

            // REQUIRES_NEW en registrar() garantiza que el audit se persiste
            // independientemente de si la transacción padre hace rollback
            auditService.registrar(
                    "Empresa \"" + nombreEmpresa + "\" aprobada", "success");

        } else {
            // Rechazo: capturar datos antes de borrar, luego hard delete
            Company empresa = findOrThrow(id);

            if (empresa.getEstadoSolicitud() != EstadoSolicitud.PENDIENTE) {
                throw new BusinessException(
                        "Solo se pueden rechazar empresas en estado PENDIENTE");
            }

            // Notificar por email antes de borrar.
            // El email está fuera del contrato transaccional — ver
            // docs/email-transactional-pattern.md.
            List<User> usuarios = userRepository.findAllByEmpresaIdAndActivoFalse(empresa.getEmpresaId());
            for (User u : usuarios) {
                try {
                    emailService.enviarRechazo(u.getEmail(), u.getNombre(), empresa.getNombreEmpresa());
                } catch (MailException ex) {
                    log.warn("No se pudo enviar email de rechazo a {} — empresa={}: {}",
                            u.getEmail(), empresa.getNombreEmpresa(), ex.getMessage());
                }
            }

            String nombreEmpresa = empresa.getNombreEmpresa();

            // Hard delete: primero usuarios (FK), luego empresa
            userRepository.deleteAllByEmpresaId(empresa.getEmpresaId());
            companyRepository.delete(empresa);

            auditService.registrar(
                    "Solicitud de \"" + nombreEmpresa + "\" rechazada y eliminada", "warning");
        }
    }


    // ── HELPER ───────────────────────────────────────────────────────────────

    private Company findOrThrow(UUID id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Empresa no encontrada con id: " + id));
    }
}