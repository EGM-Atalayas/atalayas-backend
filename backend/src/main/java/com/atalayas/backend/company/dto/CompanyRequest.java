package com.atalayas.backend.company.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload para crear una empresa (solo SUPER_ADMIN).
 * Los campos coinciden exactamente con las columnas editables de la tabla empresa.
 */
@Data
public class CompanyRequest {

    @NotBlank(message = "El nombre de la empresa es obligatorio")
    @Size(max = 200)
    private String nombreEmpresa;

    @NotBlank(message = "El CIF es obligatorio")
    @Size(max = 20)
    private String cif;

    @Size(max = 100)
    private String sector;

    @NotBlank(message = "El email de contacto es obligatorio")
    @Email(message = "El email de contacto no tiene un formato válido")
    @Size(max = 255)
    private String emailContacto;

    @Size(max = 20)
    private String telefonoContacto;

    private String descripcion;
}



