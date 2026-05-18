package com.atalayas.backend.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

/**
 * Payload para que un admin cree un usuario directamente desde el panel.
 *
 * - ROLE_ADMIN_EMPRESA: el empresaId del request se ignora; siempre se usa el del admin autenticado.
 * - ROLE_ADMIN: el empresaId es obligatorio; puede crear usuarios en cualquier empresa.
 */
@Data
public class CreateUserRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "Los apellidos son obligatorios")
    private String apellidos;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email inválido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @NotNull(message = "El rol es obligatorio")
    private UUID rolId;

    /**
     * Solo requerido cuando el que crea es ROLE_ADMIN.
     * ROLE_ADMIN_EMPRESA siempre hereda su propio empresaId; este campo se ignora.
     */
    private UUID empresaId;

    private String puestoTrabajo;

    @Size(max = 50)
    private String departamento;
}
