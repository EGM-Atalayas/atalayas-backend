package com.atalayas.backend.company.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload público para solicitar el alta de una empresa y crear su usuario admin
 * en un único paso. La empresa queda en estado PENDIENTE y el usuario con activo=false
 * hasta que el SUPER_ADMIN apruebe la solicitud.
 */
@Data
public class SolicitudAltaEmpresaRequest {

    // ── Datos de la empresa ──────────────────────────────────────────────────

    @NotBlank(message = "El nombre de la empresa es obligatorio")
    @Size(max = 200)
    private String nombreEmpresa;

    @NotBlank(message = "El CIF es obligatorio")
    @Size(max = 20)
    private String cif;

    @NotBlank(message = "El email de contacto de la empresa es obligatorio")
    @Email(message = "El email de contacto no tiene un formato válido")
    @Size(max = 255)
    private String emailContacto;

    @Size(max = 100)
    private String sector;

    @Size(max = 20)
    private String telefonoContacto;

    private String descripcion;

    // ── Datos del usuario administrador de la empresa ────────────────────────

    @NotBlank(message = "El nombre del administrador es obligatorio")
    @Size(max = 100)
    private String nombre;

    @NotBlank(message = "Los apellidos del administrador son obligatorios")
    @Size(max = 100)
    private String apellidos;

    @NotBlank(message = "El email del administrador es obligatorio")
    @Email(message = "El email del administrador no tiene un formato válido")
    @Size(max = 255)
    private String emailAdmin;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;
}

