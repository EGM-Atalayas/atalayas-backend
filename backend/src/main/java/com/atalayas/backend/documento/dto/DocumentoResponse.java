package com.atalayas.backend.documento.dto;

import com.atalayas.backend.documento.enums.TipoDocumento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Vista completa de un documento. Se usa tanto en respuestas de admin
 * como en el listado del empleado (rellenando los campos de asignación
 * cuando aplique).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoResponse {

    private UUID documentoId;
    private UUID empresaId;
    private String titulo;
    private String descripcion;
    private TipoDocumento tipo;
    private String archivoUrl;
    private String archivoNombre;
    private String mimeType;
    private Long tamanoBytes;
    private UUID subidoPor;
    private String subidoPorNombre;
    private boolean requiereFirma;
    private boolean activo;
    private OffsetDateTime fechaSubida;

    // Solo se rellenan en endpoints orientados al empleado
    private UUID asignacionId;
    private Boolean visto;
    private OffsetDateTime fechaVisto;
    private Boolean firmado;
    private OffsetDateTime fechaFirma;
    private String firmaUrl;

    // Solo se rellenan en endpoints orientados al admin
    private Integer totalAsignados;
    private Integer totalVistos;
    private Integer totalFirmados;
}
