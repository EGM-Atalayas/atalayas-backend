package com.atalayas.backend.documento.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Body para añadir asignaciones a un documento ya existente.
 * Reutiliza la misma lógica de resolución de destinatarios que la subida inicial.
 */
@Getter
@Setter
public class DocumentoAsignarRequest {

    private boolean asignarATodos = false;
    private boolean notificar = true;
    private List<UUID> usuariosIds;
    private List<String> departamentos;
}
