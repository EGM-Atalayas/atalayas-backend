package com.atalayas.backend.incidencia.mapper;

import com.atalayas.backend.incidencia.dto.IncidenciaRequest;
import com.atalayas.backend.incidencia.dto.IncidenciaResponse;
import com.atalayas.backend.incidencia.entity.Incidencia;
import com.atalayas.backend.incidencia.enums.PrioridadIncidencia;
import org.springframework.stereotype.Component;

@Component
public class IncidenciaMapper {

    public Incidencia toEntity(IncidenciaRequest request) {
        return Incidencia.builder()
                .titulo(request.getTitulo())
                .descripcion(request.getDescripcion())
                .prioridad(request.getPrioridad() != null ? request.getPrioridad() : PrioridadIncidencia.NORMAL)
                .empresaId(request.getEmpresaId())
                .build();
    }

    public IncidenciaResponse toResponse(Incidencia i) {
        return IncidenciaResponse.builder()
                .id(i.getId())
                .titulo(i.getTitulo())
                .descripcion(i.getDescripcion())
                .estado(i.getEstado())
                .prioridad(i.getPrioridad())
                .empresaId(i.getEmpresaId())
                .creadoEn(i.getCreadoEn())
                .build();
    }
}

