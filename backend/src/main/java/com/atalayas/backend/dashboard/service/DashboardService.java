package com.atalayas.backend.dashboard.service;

import com.atalayas.backend.audit.service.AuditService;
import com.atalayas.backend.common.enums.EstadoSolicitud;
import com.atalayas.backend.common.util.SecurityUtils;
import com.atalayas.backend.company.entity.Company;
import com.atalayas.backend.company.repository.CompanyRepository;
import com.atalayas.backend.dashboard.dto.ActividadRecienteDto;
import com.atalayas.backend.dashboard.dto.AdminEmpresaResumenResponse;
import com.atalayas.backend.dashboard.dto.SuperAdminDashboardResponse;
import com.atalayas.backend.dashboard.dto.SuperAdminResumenResponse;
import com.atalayas.backend.exception.ResourceNotFoundException;
import com.atalayas.backend.incidencia.enums.EstadoIncidencia;
import com.atalayas.backend.incidencia.enums.PrioridadIncidencia;
import com.atalayas.backend.incidencia.repository.IncidenciaRepository;
import com.atalayas.backend.module.repository.ModuleRepository;
import com.atalayas.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CompanyRepository    companyRepository;
    private final UserRepository       userRepository;
    private final ModuleRepository     moduleRepository;
    private final IncidenciaRepository incidenciaRepository;
    private final AuditService         auditService;

    /**
     * Resumen para ROLE_ADMIN_EMPRESA.
     * Lanza ResourceNotFoundException (404) si la empresa del usuario no existe en BD.
     */
    @Transactional(readOnly = true)
    public AdminEmpresaResumenResponse getAdminEmpresaResumen() {
        UUID empresaId = SecurityUtils.getEmpresaId();

        Company empresa = companyRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Empresa no encontrada con id: " + empresaId));

        long activos   = userRepository.countByEmpresaIdAndActivoTrue(empresaId);
        long inactivos = userRepository.countByEmpresaIdAndActivoFalse(empresaId);

        return AdminEmpresaResumenResponse.builder()
                .nombreEmpresa(empresa.getNombreEmpresa())
                .usuariosActivos(activos)
                .usuariosInactivos(inactivos)
                .build();
    }

    /**
     * Resumen para ROLE_ADMIN (superadmin EGM).
     * Agrega contadores globales de toda la plataforma.
     */
    @Transactional(readOnly = true)
    public SuperAdminResumenResponse getSuperAdminResumen() {
        long totalEmpresas      = companyRepository.count();
        long pendientes         = companyRepository.countByEstadoSolicitud(EstadoSolicitud.PENDIENTE);
        long aprobadas          = companyRepository.countByEstadoSolicitud(EstadoSolicitud.APROBADA);
        long totalUsuarios      = userRepository.count();

        return SuperAdminResumenResponse.builder()
                .totalEmpresas(totalEmpresas)
                .empresasPendientes(pendientes)
                .empresasAprobadas(aprobadas)
                .totalUsuarios(totalUsuarios)
                .build();
    }

    /**
     * Dashboard completo para ROLE_ADMIN (superadmin EGM).
     * GET /api/v1/dashboard/superadmin
     */
    @Transactional(readOnly = true)
    public SuperAdminDashboardResponse getSuperAdminDashboard() {
        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        long empresasAdheridas    = companyRepository.count();
        long empresasNuevasMes    = companyRepository.countByFechaSolicitudAfter(inicioMes);
        long empleadosRegistrados = userRepository.count();
        long empleadosNuevosMes   = userRepository.countByFechaRegistroAfter(inicioMes);
        long modulosPublicados    = moduleRepository.countByActivoTrue();
        long incidenciasAbiertas  = incidenciaRepository.countByEstado(EstadoIncidencia.ABIERTA);
        long incidenciasCriticas  = incidenciaRepository.countByEstadoAndPrioridad(
                                        EstadoIncidencia.ABIERTA, PrioridadIncidencia.CRITICA);
        List<ActividadRecienteDto> actividad = auditService.getActividadReciente();

        return SuperAdminDashboardResponse.builder()
                .empresasAdheridas(empresasAdheridas)
                .empresasNuevasMes(empresasNuevasMes)
                .empleadosRegistrados(empleadosRegistrados)
                .empleadosNuevosMes(empleadosNuevosMes)
                .modulosPublicados(modulosPublicados)
                .incidenciasAbiertas(incidenciasAbiertas)
                .incidenciasCriticas(incidenciasCriticas)
                .actividadReciente(actividad)
                .build();
    }
}

