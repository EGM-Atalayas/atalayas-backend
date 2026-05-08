package com.atalayas.backend.servicios.mapper;

import com.atalayas.backend.servicios.dto.ServicioRequest;
import com.atalayas.backend.servicios.dto.ServicioResponse;
import com.atalayas.backend.servicios.entity.Servicio;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mapper entre Servicio y sus DTOs
 */
@Component
public class ServicioMapper {

    public Servicio toEntity(ServicioRequest request, UUID creadoPor) {
        return Servicio.builder()
                .titulo(request.getTitulo())
                .descripcion(request.getDescripcion())
                .categoria(request.getCategoria())
                .iconoUrl(request.getIconoUrl())
                .urlInfo(request.getUrlInfo())
                .telefono(request.getTelefono())
                .comoAcceder(request.getComoAcceder())
                .creadoPor(creadoPor)
                .activo(true)
                .build();
    }

    public ServicioResponse toResponse(Servicio s) {
        return ServicioResponse.builder()
                .servicioId(s.getServicioId())
                .titulo(s.getTitulo())
                .descripcion(s.getDescripcion())
                .categoria(s.getCategoria())
                .iconoUrl(s.getIconoUrl())
                .urlInfo(s.getUrlInfo())
                .telefono(s.getTelefono())
                .comoAcceder(s.getComoAcceder())
                .creadoPor(s.getCreadoPor())
                .activo(s.isActivo())
                .creadoEn(s.getCreadoEn())
                .actualizadoEn(s.getActualizadoEn())
                .build();
    }
}
