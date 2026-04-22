package com.atalayas.backend.module.entity;

import com.atalayas.backend.common.enums.ModuleType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad mapeada a la tabla 'modulo'
 *
 * Un módulo agrupa contenidos formativos por tipo y orden de visualización
 * Puede ser global (empresaId = null) y visible para todas las empresas,
 * o específico de una empresa concreta
 *
 * Los módulos globales los gestiona EGM, los de empresa, el admin empresa
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

    // Descripción opcional para dar contexto al empleado antes de entrar
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    // null = módulo global disponible para todas las empresas del parque
    @Column(name = "empresa_id")
    private UUID empresaId;

    // Tipo que determina la categoría del módulo — INCORPORACION, FORMACION, etc.
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_modulo", nullable = false, length = 50)
    private ModuleType tipoModulo;

    // Posición del módulo en el listado — ordena la visualización en Mi formación
    @Column(name = "orden")
    private Integer orden;

    // Indica si el módulo fue generado o asistido por IA
    @Column(name = "es_especializado_ia", nullable = false)
    @Builder.Default
    private boolean esEspecializadoIa = false;

    // Idioma del módulo (es, en, ca…)
    @Column(name = "idioma", length = 10)
    @Builder.Default
    private String idioma = "es";

    // Duración estimada del módulo (corto, medio, largo)
    @Column(name = "duracion", length = 20)
    private String duracion;

    // Audiencia: todos | administradores | departamento
    @Column(name = "audiencia", length = 30)
    @Builder.Default
    private String audiencia = "todos";

    // Departamentos destinatarios cuando audiencia = departamento
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "departamentos", columnDefinition = "text[]")
    private String[] departamentos;

    // Preguntas del test en formato JSON
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "test_preguntas", columnDefinition = "jsonb")
    private String testPreguntas;

    // Soft delete — false oculta el módulo para empleados pero conserva datos históricos
    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    // Gestionado automáticamente
    @Column(name = "fecha_creacion", updatable = false, nullable = false)
    private OffsetDateTime fechaCreacion;

    // Gestionado automáticamente
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = OffsetDateTime.now();
        actualizadoEn = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = OffsetDateTime.now();
    }
}