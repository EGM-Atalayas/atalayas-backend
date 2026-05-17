package com.atalayas.backend.documento.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Body para eliminar asignaciones concretas de un documento.
 * Se pasan los asignacionIds a borrar (obtenidos del listado de asignaciones).
 */
@Getter
@Setter
public class DocumentoDesasignarRequest {

    @NotEmpty(message = "Debes indicar al menos una asignación a eliminar")
    private List<UUID> asignacionIds;
}
