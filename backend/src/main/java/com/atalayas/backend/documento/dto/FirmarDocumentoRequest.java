package com.atalayas.backend.documento.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Body del endpoint POST /documentos/me/{id}/firmar.
 * El frontend envía la firma como imagen PNG codificada en base64
 * (salida de canvas.toDataURL("image/png")).
 */
@Getter
@Setter
public class FirmarDocumentoRequest {

    /** PNG en base64, con o sin el prefijo "data:image/png;base64," */
    @NotBlank
    private String firmaBase64;
}
