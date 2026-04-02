package com.atalayas.backend.dashboard.service;

import com.atalayas.backend.common.enums.EstadoSolicitud;
import com.atalayas.backend.common.util.SecurityUtils;
import com.atalayas.backend.company.entity.Company;
import com.atalayas.backend.company.repository.CompanyRepository;
import com.atalayas.backend.dashboard.dto.AdminEmpresaResumenResponse;
import com.atalayas.backend.dashboard.dto.SuperAdminResumenResponse;
import com.atalayas.backend.exception.ResourceNotFoundException;
import com.atalayas.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

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
}

