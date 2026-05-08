package com.atalayas.backend.user.dto;

import com.atalayas.backend.user.enums.Disponibilidad;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(max = 100)
    private String nombre;

    @Size(max = 100)
    private String apellidos;

    @Size(max = 150)
    private String puestoTrabajo;

    @Size(max = 500)
    private String avatarUrl;

    @Size(max = 500)
    private String bannerUrl;

    @Size(max = 300)
    private String bio;

    @Size(max = 20)
    private String telefono;

    private Disponibilidad disponibilidad;

    private Boolean notifNuevoModulo;
    private Boolean notifModuloCompletado;
    private Boolean notifComunicado;
    private Boolean notifPendiente;
    private Boolean modoOscuro;
}
