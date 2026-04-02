package com.atalayas.backend.content.entity;

import com.atalayas.backend.common.enums.ContentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;


/**
 * Entidad que representa un contenido formativo en la tabla 'contenido'
 *
 * Un contenido pertenece siempre a un módulo y opcionalmente a una empresa
 * Puede ser texto, vídeo, PDF, evaluación o contenido generado por IA
 */
@Entity
@Table(name = "contenido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "contenido_id", updatable = false, nullable = false)
    private UUID contenidoId;

    // FK al módulo al que pertenece este contenido
    @Column(name = "modulo_id", nullable = false)
    private UUID moduloId;

    // null = contenido global (visible para todas las empresas)
    @Column(name = "empresa_id")
    private UUID empresaId;

    // Título visible para el empleado en la plataforma
    @Column(name = "titulo", nullable = false, length = 255)
    private String titulo;

    // Descripción corta opcional para mostrar en listados
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    // Tipo que determina cómo se renderiza el contenido en el frontend
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_contenido", nullable = false, length = 50)
    private ContentType tipoContenido;

    // URL externa del recurso (vídeo, PDF, etc.) - null si es TEXTO
    @Column(name = "url_recurso", length = 500)
    private String urlRecurso;

    // Cuerpo del contenido cuando es de tipo TEXTO o IA_GENERADO
    @Column(name = "cuerpo_texto", columnDefinition = "TEXT")
    private String cuerpoTexto;

    // Posición dentro del módulo para ordenar la visualización
    @Column(name = "orden", nullable = false)
    @Builder.Default
    private int orden = 0;

    // Control de versiones: se incrementa al actualizar contenido importante
    @Column(name = "version", nullable = false)
    @Builder.Default
    private int version = 1;

    // Tiempo estimado de lectura/visualización en minutos
    @Column(name = "minutos_estimados")
    private Integer minutosEstimados;

    // Indica si este contenido fue generado por IA
    @Column(name = "es_ia_generado", nullable = false)
    @Builder.Default
    private boolean esIaGenerado = false;

    // Soft delete: conserva trazabilidad histórica aunque no sea visible
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