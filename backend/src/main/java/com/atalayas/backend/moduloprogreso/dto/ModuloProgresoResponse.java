package com.atalayas.backend.moduloprogreso.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Respuesta de los endpoints de progreso de módulo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModuloProgresoResponse {
    private UUID            moduloId;
    private int             contenidosCompletados;
    private int             totalContenidos;
    private int             porcentaje;
    private boolean         completado;
    private OffsetDateTime  fechaInicio;
    private OffsetDateTime  fechaCompletado;
    private OffsetDateTime  actualizadoEn;
}
