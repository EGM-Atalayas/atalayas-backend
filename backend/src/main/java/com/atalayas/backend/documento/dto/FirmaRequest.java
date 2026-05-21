package com.atalayas.backend.documento.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Body del endpoint POST /documentos/me/{id}/firmar
 * firmaBase64: imagen PNG de la firma manuscrita, en Base64 (sin prefijo data:...).
 */
@Getter
@Setter
public class FirmaRequest {

    @NotBlank(message = "La firma no puede estar vacía")
    private String firmaBase64;
}
