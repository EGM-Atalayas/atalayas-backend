package com.atalayas.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Optional;

@Data
public class UpdateUserRequest {

    @Size(max = 100)
    private String nombre;

    @Size(max = 100)
    private String apellidos;

    @Email
    @Size(max = 255)
    private String email;

    private Optional<String> puestoTrabajo;

    private Optional<String> departamento;
}
