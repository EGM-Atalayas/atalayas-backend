package com.atalayas.backend.community.mapper;

import com.atalayas.backend.community.dto.CommunityEventRequest;
import com.atalayas.backend.community.dto.CommunityEventResponse;
import com.atalayas.backend.community.entity.CommunityEvent;
import com.atalayas.backend.usuario.entity.User;
import org.springframework.stereotype.Component;

import java.util.UUID;


/**
 * Mapper para convertir entre CommunityEvent y sus DTOs
 * La resolución de esGlobal y empresaId según el rol del usuario
 * se hace siempre en el service antes de llamar al mapper
 */
@Component
public class CommunityEventMapper {


    /**
     * Construye un CommunityEvent listo para persistir
     *
     * @param request   payload validado del controller
     * @param empresaId empresa ya resuelta según el rol del usuario
     * @param esGlobal  flag ya resuelto según el rol del usuario
     * @param creadoPor ID del usuario que crea el evento
     */
    public CommunityEvent toEntity(CommunityEventRequest request,
                                   UUID empresaId,
                                   boolean esGlobal,
                                   UUID creadoPor) {
        return CommunityEvent.builder()
                .titulo(request.getTitulo())
                .descripcion(request.getDescripcion())
                .empresaId(empresaId)
                .esGlobal(esGlobal)
                .fechaInicio(request.getFechaInicio())
                .fechaFin(request.getFechaFin())
                .lugar(request.getLugar())
                .latitud(request.getLatitud())
                .longitud(request.getLongitud())
                .imagenUrl(request.getImagenUrl())
                .creadoPor(creadoPor)
                .activo(true)
                .build();
    }


    /**
     * Convierte un CommunityEvent a su DTO de respuesta
     */
    public CommunityEventResponse toResponse(CommunityEvent e) {
        return CommunityEventResponse.builder()
                .eventoId(e.getEventoId())
                .empresaId(e.getEmpresaId())
                .creadoPor(e.getCreadoPor())
                .titulo(e.getTitulo())
                .descripcion(e.getDescripcion())
                .esGlobal(e.isEsGlobal())
                .activo(e.isActivo())
                .fechaInicio(e.getFechaInicio())
                .fechaFin(e.getFechaFin())
                .lugar(e.getLugar())
                .latitud(e.getLatitud())
                .longitud(e.getLongitud())
                .imagenUrl(e.getImagenUrl())
                .creadoEn(e.getCreadoEn())
                .actualizadoEn(e.getActualizadoEn())
                .build();
    }
}