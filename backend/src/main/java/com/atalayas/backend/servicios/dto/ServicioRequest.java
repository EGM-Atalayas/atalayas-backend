package com.atalayas.backend.servicios.dto;

import com.atalayas.backend.servicios.enums.CategoriaServicio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Payload para crear o actualizar un servicio del área empresarial
 */
@Data
public class ServicioRequest {

    @NotBlank(message = "El título es obligatorio")
    private String titulo;

    private String descripcion;

    @NotNull(message = "La categoría es obligatoria")
    private CategoriaServicio categoria;

    private String iconoUrl;

    private String urlInfo;

    private String telefono;

    private String comoAcceder;
}
