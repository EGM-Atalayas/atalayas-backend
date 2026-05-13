package com.atalayas.backend.documento.dto;

import com.atalayas.backend.documento.enums.TipoDocumento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Payload del formulario multipart cuando un admin sube un documento
 * nuevo. Los IDs de empleado pueden omitirse si se asigna por
 * departamento o a toda la empresa.
 */
@Data
@NoArgsConstructor
public class DocumentoUploadRequest {

    @NotBlank
    @Size(max = 200)
    private String titulo;

    @Size(max = 500)
    private String descripcion;

    @NotNull
    private TipoDocumento tipo;

    private boolean requiereFirma;

    /** IDs concretos de empleado. Vacío si se asigna por dpto o a todos */
    private List<UUID> usuariosIds;

    /** Códigos de departamento (PRODUCCION, RRHH, ...). Vacío si no aplica */
    private List<String> departamentos;

    /** true = asignar a todos los empleados activos de la empresa */
    private boolean asignarATodos;

    /** true = mandar notificación interna al/los empleado/s asignado/s */
    private boolean notificar = true;
}
