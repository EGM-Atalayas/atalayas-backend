package com.atalayas.backend.module.entity;

import com.atalayas.backend.common.enums.ModuleType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;


/**
 * Entidad que representa un módulo formativo en la tabla 'modulo'
 * Un módulo puede ser global (empresaId = null) o pertenecer a una empresa concreta
 * Agrupa contenidos formativos por tipo y orden de visualización
 */
@Entity
@Table(name = "modulo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingModule {


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "modulo_id", updatable = false, nullable = false)
    private UUID moduloId;

    // Nombre visible del módulo en la plataforma
    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    // Descripción opcional para dar contexto al empleado
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    // null = módulo global disponible para todas las empresas
    @Column(name = "empresa_id")
    private UUID empresaId;

    // Tipo que determina la categoría y visibilidad del módulo
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_modulo", nullable = false, length = 50)
    private ModuleType tipoModulo;

    // Posición del módulo en el listado de la empresa
    @Column(name = "orden")
    private Integer orden;

    // Indica si el módulo fue generado o asistido por IA
    @Column(name = "es_especializado_ia", nullable = false)
    @Builder.Default
    private boolean esEspecializadoIa = false;

    // Soft delete: false = oculto para empleados pero conserva datos históricos
    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(name = "fecha_creacion", updatable = false, nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        actualizadoEn = LocalDateTime.now();
    }


    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = LocalDateTime.now();
    }
}
