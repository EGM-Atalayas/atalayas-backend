package com.atalayas.backend.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Payload para PATCH /api/v1/empresas/{id}/solicitud.
 * El campo {@code accion} acepta "aprobar" o "rechazar".
 */
@Data
public class AccionSolicitudRequest {

    @NotBlank(message = "La acción es obligatoria")
    @Pattern(regexp = "aprobar|rechazar",
             message = "La acción debe ser 'aprobar' o 'rechazar'")
    private String accion;
}

