package com.atalayas.backend.sugerencia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SugerenciaRequest(
    @NotBlank(message = "El mensaje no puede estar vacío")
    @Size(max = 500, message = "Máximo 500 caracteres")
    String mensaje,

    @NotNull(message = "El destinatario es obligatorio")
    DestinatarioSugerencia destinatario
) {}
