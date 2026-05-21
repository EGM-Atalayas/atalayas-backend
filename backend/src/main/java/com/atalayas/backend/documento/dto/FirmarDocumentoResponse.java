package com.atalayas.backend.documento.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Respuesta del endpoint POST /documentos/me/{id}/firmar.
 * Devuelve la URL del PDF firmado almacenado en Supabase.
 */
@Getter
@AllArgsConstructor
public class FirmarDocumentoResponse {
    private String firmaUrl;
}
