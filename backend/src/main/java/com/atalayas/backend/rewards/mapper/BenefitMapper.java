package com.atalayas.backend.rewards.mapper;

import com.atalayas.backend.rewards.dto.BenefitRequest;
import com.atalayas.backend.rewards.dto.BenefitResponse;
import com.atalayas.backend.rewards.entity.Benefit;
import org.springframework.stereotype.Component;

import java.util.UUID;


/**
 * Mapper para convertir entre Benefit y sus DTOs
 * La empresaId ya viene resuelta desde el service según el rol del usuario
 */
@Component
public class BenefitMapper {


    /**
     * Construye un Benefit listo para persistir
     *
     * @param request   payload validado del controller
     * @param empresaId empresa ya resuelta - null si es beneficio global
     * @param creadoPor ID del usuario que crea el beneficio
     */
    public Benefit toEntity(BenefitRequest request, UUID empresaId, UUID creadoPor) {
        return Benefit.builder()
                .titulo(request.getTitulo())
                .descripcion(request.getDescripcion())
                .urlInfo(request.getUrlInfo())
                .empresaId(empresaId)
                .creadoPor(creadoPor)
                .activo(true)
                .build();
    }


    /**
     * Convierte un Benefit a su DTO de respuesta
     */
    public BenefitResponse toResponse(Benefit b) {
        return BenefitResponse.builder()
                .beneficioId(b.getBeneficioId())
                .empresaId(b.getEmpresaId())
                .creadoPor(b.getCreadoPor())
                .titulo(b.getTitulo())
                .descripcion(b.getDescripcion())
                .urlInfo(b.getUrlInfo())
                .activo(b.isActivo())
                .creadoEn(b.getCreadoEn())
                .actualizadoEn(b.getActualizadoEn())
                .build();
    }
}