package com.atalayas.backend.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;


/**
 * Payload para crear o actualizar un evento de comunidad
 * Admin empresa solo puede crear eventos para su empresa (esGlobal se ignora)
 * Superadmin puede crear eventos globales visibles para toda la plataforma
 */
@Data
public class CommunityEventRequest {

    @NotBlank(message = "El título es obligatorio")
    private String titulo;

    private String descripcion;

    // Solo superadmin puede crear eventos globales — admin empresa lo ignora
    private boolean esGlobal = false;

    // Empresa propietaria - se sobreescribe con la del usuario si es admin empresa
    private UUID empresaId;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDateTime fechaInicio;

    // Fecha de fin opcional - null = evento sin fecha límite
    private LocalDateTime fechaFin;
}