package com.atalayas.backend.progress.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;


/**
 * Entidad que representa la trazabilidad de un empleado sobre un contenido
 * Mapeada a la tabla 'trazabilidad_lectura'
 *
 * Cada registro es único por (usuario_id, contenido_id) — un empleado
 * solo tiene un registro por contenido que se va actualizando conforme avanza.
 * El campo 'estado' no se persiste: se deriva en servicio a partir de
 * 'completado' y 'tiempoSegundos'
 */
@Entity
@Table(
        name = "trazabilidad_lectura",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_trazabilidad_usuario_contenido",
                columnNames = {"usuario_id", "contenido_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "registro_id", updatable = false, nullable = false)
    private UUID registroId;

    // FK al empleado que está completando el contenido
    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    // FK al contenido que se está trazando
    @Column(name = "contenido_id", nullable = false)
    private UUID contenidoId;

    // FK a la empresa del empleado — facilita queries de dashboard por empresa
    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    // true cuando el empleado marca el contenido como terminado
    @Column(name = "completado", nullable = false)
    @Builder.Default
    private boolean completado = false;

    // Fecha en que se marcó como completado - null si aún no terminó
    @Column(name = "fecha_completado")
    private LocalDateTime fechaCompletado;

    // Tiempo acumulado de visualización/lectura en segundos
    @Column(name = "tiempo_segundos", nullable = false)
    @Builder.Default
    private int tiempoSegundos = 0;

    // Versión del contenido que leyó el empleado - permite detectar si hay versión nueva
    @Column(name = "version_leida", nullable = false)
    @Builder.Default
    private int versionLeida = 1;

    // Hash de aceptación o firma digital al completar (para cumplimiento normativo)
    @Column(name = "hash_aceptacion", length = 500)
    private String hashAceptacion;

    // Fecha de primera apertura del contenido
    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;


    @PrePersist
    protected void onCreate() {
        fechaInicio = LocalDateTime.now();
        actualizadoEn = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = LocalDateTime.now();
    }
}