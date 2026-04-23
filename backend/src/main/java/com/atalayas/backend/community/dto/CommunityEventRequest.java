package com.atalayas.backend.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Payload para crear o actualizar un evento de comunidad
 *
 * El admin empresa solo puede crear eventos para su propia empresa,
 * el campo esGlobal se ignora si el usuario es ROLE_ADMIN_EMPRESA
 * Solo ROLE_ADMIN puede crear eventos globales visibles para toda la plataforma
 */
@Data
public class CommunityEventRequest {

    @NotBlank(message = "El título es obligatorio")
    private String titulo;

    private String descripcion;

    // Solo superadmin puede poner esto a true
    private boolean esGlobal = false;

    // Empresa propietaria
    private UUID empresaId;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private OffsetDateTime fechaInicio;

    // Fecha de fin opcional - null = evento sin fecha límite
    private OffsetDateTime fechaFin;
}