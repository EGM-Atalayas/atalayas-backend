package com.atalayas.backend.company.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa el campo JSON contexto_industrial de la tabla empresa.
 * Se serializa / deserializa directamente como JsonNode para permitir
 * cualquier estructura JSON libre definida por empresa.
 *
 * Ejemplo de contenido esperado:
 * {
 *   "normativas": ["ISO 9001", "ISO 14001"],
 *   "procesos_clave": ["fabricacion", "logistica"],
 *   "riesgos": ["contaminacion", "accidente laboral"]
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndustrialContextDto {

    /**
     * Contenido JSON libre del contexto industrial.
     * Se almacena como JSONB en PostgreSQL y se mapea con
     * @Type(JsonBinaryType.class) en la entidad Empresa.
     */
    private JsonNode datos;
}

