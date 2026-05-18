package com.atalayas.backend.usuario.departamento.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Departamento al que puede pertenecer un usuario.
 * Puede ser global (empresaId = null, gestionado por EGM)
 * o específico de una empresa (empresaId != null).
 */
@Entity
@Table(name = "departamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
