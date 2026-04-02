package com.atalayas.backend.company.dto;

import com.atalayas.backend.common.enums.EstadoSolicitud;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Payload para el endpoint PATCH /empresas/{id}/estado.
 * Permite al SUPER_ADMIN cambiar el estado de una empresa respetando
 * las transiciones válidas definidas en la lógica de negocio:
 *   PENDIENTE  → APROBADA  | RECHAZADA
 *   RECHAZADA  → PENDIENTE
 *   APROBADA   → (ninguna transición permitida)
 */
@Data
public class CambioEstadoRequest {

    @NotNull(message = "El nuevo estado es obligatorio")
    private EstadoSolicitud nuevoEstado;
}

