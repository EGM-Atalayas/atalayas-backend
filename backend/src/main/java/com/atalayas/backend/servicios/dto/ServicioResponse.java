package com.atalayas.backend.servicios.dto;

import com.atalayas.backend.servicios.enums.CategoriaServicio;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Respuesta de un servicio del área empresarial
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicioResponse {

    private UUID servicioId;
    private String titulo;
    private String descripcion;
    private CategoriaServicio categoria;
    private String iconoUrl;
    private String urlInfo;
    private String telefono;
    private String comoAcceder;
    private UUID creadoPor;
    private boolean activo;
    private OffsetDateTime creadoEn;
    private OffsetDateTime actualizadoEn;
}
