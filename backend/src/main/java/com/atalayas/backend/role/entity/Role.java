package com.atalayas.backend.role.entity;

import com.atalayas.backend.common.enums.RoleType;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad mapeada a la tabla 'rol' de la BBDD
 *
 * Cada usuario tiene un rol que determina qué puede
 * ver y hacer dentro de la plataforma
 */
@Entity
@Table(name = "rol")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "rol_id", updatable = false, nullable = false)
    private UUID rolId;

    // Nombre legible del rol
    @Column(name = "nombre_rol", nullable = false, unique = true, length = 50)
    private String nombreRol;

    /**
     * Código técnico del rol
     * ROLE_ADMIN, ROLE_ADMIN_EMPRESA, ROLE_EMPLEADO
     */
    @Column(name = "codigo_rol", nullable = false, unique = true, length = 50)
    private String codigoRol;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    // Gestionado automáticamente
    @Column(name = "actualizado_en", nullable = false)
    @Builder.Default
    private OffsetDateTime actualizadoEn = OffsetDateTime.now();

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = OffsetDateTime.now();
    }

    /**
     * Resuelve el código de rol a su enum RoleType equivalente.
     */
    public RoleType getRoleType() {
        return RoleType.fromCodigo(codigoRol);
    }
}