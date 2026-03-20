package com.atalayas.backend.company.mapper;

import com.atalayas.backend.company.dto.CompanyRequest;
import com.atalayas.backend.company.dto.CompanyResponse;
import com.atalayas.backend.company.dto.SolicitudAltaEmpresaRequest;
import com.atalayas.backend.company.dto.SolicitudAltaEmpresaResponse;
import com.atalayas.backend.company.entity.Company;
import com.atalayas.backend.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {

    public CompanyResponse toResponse(Company company) {
        return CompanyResponse.builder()
                .empresaId(company.getEmpresaId())
                .nombreEmpresa(company.getNombreEmpresa())
                .cif(company.getCif())
                .sector(company.getSector())
                .emailContacto(company.getEmailContacto())
                .telefonoContacto(company.getTelefonoContacto())
                .descripcion(company.getDescripcion())
                .estadoSolicitud(company.getEstadoSolicitud())
                .activo(company.isActivo())
                .fechaSolicitud(company.getFechaSolicitud())
                .fechaResolucion(company.getFechaResolucion())
                .actualizadoEn(company.getActualizadoEn())
                .build();
    }

    public Company toEntity(CompanyRequest request) {
        return Company.builder()
                .nombreEmpresa(request.getNombreEmpresa())
                .cif(request.getCif())
                .sector(request.getSector())
                .emailContacto(request.getEmailContacto())
                .telefonoContacto(request.getTelefonoContacto())
                .descripcion(request.getDescripcion())
                .build();
    }

    /** Construye una Company desde el payload de alta pública. */
    public Company toEntityFromSolicitud(SolicitudAltaEmpresaRequest request) {
        return Company.builder()
                .nombreEmpresa(request.getNombreEmpresa())
                .cif(request.getCif())
                .sector(request.getSector())
                .emailContacto(request.getEmailContacto())
                .telefonoContacto(request.getTelefonoContacto())
                .descripcion(request.getDescripcion())
                .build();
    }

    /** Respuesta combinada empresa + usuario admin provisional. */
    public SolicitudAltaEmpresaResponse toSolicitudResponse(Company company, User adminUser) {
        return SolicitudAltaEmpresaResponse.builder()
                .empresaId(company.getEmpresaId())
                .nombreEmpresa(company.getNombreEmpresa())
                .cif(company.getCif())
                .sector(company.getSector())
                .emailContacto(company.getEmailContacto())
                .telefonoContacto(company.getTelefonoContacto())
                .descripcion(company.getDescripcion())
                .estadoSolicitud(company.getEstadoSolicitud())
                .fechaSolicitud(company.getFechaSolicitud())
                .usuarioId(adminUser.getUsuarioId())
                .nombreAdmin(adminUser.getNombre())
                .apellidosAdmin(adminUser.getApellidos())
                .emailAdmin(adminUser.getEmail())
                .activoAdmin(adminUser.isActivo())
                .build();
    }
}

