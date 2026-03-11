package com.atalayas.backend.role.entity;

import com.atalayas.backend.common.enums.RoleType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad mapeada a la tabla rol de PostgreSQL.
 * codigoRol se almacena como String en BD y se puede resolver
 * al enum RoleType mediante RoleType.fromCodigo(codigoRol).
 */
@Entity
@Table(name = "rol")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "rol_id", updatable = false, nullable = false)
    private UUID rolId;

    @Column(name = "nombre_rol", nullable = false, unique = true, length = 50)
    private String nombreRol;

    /**
     * Código técnico del rol. Debe coincidir con uno de los valores de RoleType.
     * Se usa con RoleType.fromCodigo(codigoRol) en la capa de servicio.
     */
    @Column(name = "codigo_rol", nullable = false, unique = true, length = 50)
    private String codigoRol;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "actualizado_en", nullable = false)
    @Builder.Default
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = LocalDateTime.now();
    }

    /** Resuelve el código a su enum RoleType correspondiente. */
    public RoleType getRoleType() {
        return RoleType.fromCodigo(codigoRol);
    }
}

