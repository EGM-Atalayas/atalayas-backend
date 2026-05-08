package com.atalayas.backend.eventos.entity;

import com.atalayas.backend.eventos.enums.EstadoEvento;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Eventos del área empresarial EGM Atalayas:
 * jornadas, actividades, encuentros, semanas temáticas...
 */
@Entity
@Table(name = "evento")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "evento_id", updatable = false, nullable = false)
    private UUID eventoId;

    @Column(name = "titulo", nullable = false, length = 255)
    private String titulo;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "hora_fin")
    private LocalTime horaFin;

    @Column(name = "lugar", length = 255)
    private String lugar;

    @Column(name = "url_info", length = 500)
    private String urlInfo;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoEvento estado = EstadoEvento.PROXIMO;

    @Column(name = "creado_por")
    private UUID creadoPor;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @PrePersist
    protected void onCreate() {
        creadoEn = OffsetDateTime.now();
        actualizadoEn = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = OffsetDateTime.now();
    }
}
