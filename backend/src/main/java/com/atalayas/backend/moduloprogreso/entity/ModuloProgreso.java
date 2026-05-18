package com.atalayas.backend.moduloprogreso.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Progreso agregado a nivel de módulo formativo para un usuario.
 *
 * Complementa a {@code trazabilidad_lectura} (que registra por contenido):
 * aquí guardamos directamente {@code completados / total / porcentaje} del
 * módulo entero, alimentado desde el frontend al marcar contenidos.
 *
 * Garantiza un único registro por (usuario, modulo).
 */
@Entity
@Table(name = "modulo_progreso",
        uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "modulo_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModuloProgreso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "progreso_id", updatable = false, nullable = false)
    private UUID progresoId;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "modulo_id", nullable = false)
    private UUID moduloId;

    /** Empresa del usuario en el momento de registrar el progreso (desnormalizado para dashboards). */
    @Column(name = "empresa_id")
    private UUID empresaId;

    @Column(name = "contenidos_completados", nullable = false)
    @Builder.Default
    private int contenidosCompletados = 0;

    @Column(name = "total_contenidos", nullable = false)
    @Builder.Default
    private int totalContenidos = 0;

    /** 0-100. Constraint en BD lo limita al rango. */
    @Column(name = "porcentaje", nullable = false)
    @Builder.Default
    private int porcentaje = 0;

    /** True cuando porcentaje >= 100. Irreversible. */
    @Column(name = "completado", nullable = false)
    @Builder.Default
    private boolean completado = false;

    @Column(name = "fecha_inicio", updatable = false)
    private OffsetDateTime fechaInicio;

    @Column(name = "fecha_completado")
    private OffsetDateTime fechaCompletado;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime ahora = OffsetDateTime.now();
        if (fechaInicio == null) fechaInicio = ahora;
        actualizadoEn = ahora;
    }

    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = OffsetDateTime.now();
    }
}
