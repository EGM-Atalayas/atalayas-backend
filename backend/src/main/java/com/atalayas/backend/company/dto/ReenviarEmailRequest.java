package com.atalayas.backend.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Cuerpo del endpoint POST /api/v1/empresas/{id}/reenviar-email.
 * El único tipo soportado en v1 es "aprobacion".
 */
@Data
public class ReenviarEmailRequest {

    @NotBlank
    @Pattern(regexp = "aprobacion", message = "Tipo debe ser 'aprobacion'")
    private String tipo;
}

