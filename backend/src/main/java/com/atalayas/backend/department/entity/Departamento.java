package com.atalayas.backend.department.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "departamento")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Departamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "departamento_id", updatable = false, nullable = false)
    private UUID departamentoId;

    @Column(name = "empresa_id")
    private UUID empresaId;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;
}
