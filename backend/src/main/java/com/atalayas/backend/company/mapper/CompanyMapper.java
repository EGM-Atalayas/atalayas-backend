package com.atalayas.backend.company.mapper;

import com.atalayas.backend.company.dto.CompanyRequest;
import com.atalayas.backend.company.dto.CompanyResponse;
import com.atalayas.backend.company.dto.SolicitudAltaEmpresaRequest;
import com.atalayas.backend.company.dto.SolicitudAltaEmpresaResponse;
import com.atalayas.backend.company.dto.SolicitudPendienteResponse;
import com.atalayas.backend.company.entity.Company;
import com.atalayas.backend.user.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper para convertir entre la entidad Company y sus DTOs.
 *
 * Sigue el mismo patrón que el resto de mappers del proyecto:
 * toEntity() para construir desde un request, toResponse() para exponer al frontend.
 */
@Component
public class CompanyMapper {

    /**
     * Convierte una entidad Company en su DTO de respuesta completo.
     * Incluye logoUrl para que el frontend pueda mostrarlo en el header.
     */
    public CompanyResponse toResponse(Company company) {
        return CompanyResponse.builder()
                .empresaId(company.getEmpresaId())
                .nombreEmpresa(company.getNombreEmpresa())
                .cif(company.getCif())
                .sector(company.getSector())
                .logoUrl(company.getLogoUrl())
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

    /**
     * Construye una Company desde un request de gestión (panel admin).
     * Los campos de auditoría los gestiona el @PrePersist de la entidad.
     */
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

    /**
     * Construye una Company desde el payload de solicitud pública de alta.
     * La empresa arranca en estado PENDIENTE y activo=false por defecto
     * hasta que el superadmin la apruebe.
     */
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

    /**
     * Respuesta combinada empresa + usuario admin provisional.
     * Se devuelve al completar el formulario de solicitud pública.
     */
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

    /**
     * Respuesta enriquecida para el listado de solicitudes pendientes.
     * Combina empresa + primer usuario admin asociado.
     */
    public SolicitudPendienteResponse toSolicitudPendienteResponse(Company company, User adminUser) {
        return SolicitudPendienteResponse.builder()
                .empresaId(company.getEmpresaId())
                .nombreEmpresa(company.getNombreEmpresa())
                .cif(company.getCif())
                .emailContacto(company.getEmailContacto())
                .estadoSolicitud(company.getEstadoSolicitud())
                .fechaSolicitud(company.getFechaSolicitud())
                .nombreAdmin(adminUser != null ? adminUser.getNombre() + " " + adminUser.getApellidos() : null)
                .emailAdmin(adminUser != null ? adminUser.getEmail() : null)
                .build();
    }
}

