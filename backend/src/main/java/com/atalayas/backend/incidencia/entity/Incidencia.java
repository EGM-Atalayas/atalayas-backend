package com.atalayas.backend.incidencia.entity;
import com.atalayas.backend.incidencia.enums.EstadoIncidencia;
import com.atalayas.backend.incidencia.enums.PrioridadIncidencia;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
/**
 * Entidad mapeada a la tabla 'incidencia'.
 * Permite al superadmin registrar y gestionar incidencias de la plataforma.
 * La tabla empieza vacía: countByEstado devuelve 0 hasta que se creen registros.
 */
@Entity
@Table(name = "incidencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incidencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "titulo", nullable = false, length = 300)
    private String titulo;
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoIncidencia estado = EstadoIncidencia.ABIERTA;
    @Enumerated(EnumType.STRING)
    @Column(name = "prioridad", nullable = false, length = 20)
    @Builder.Default
    private PrioridadIncidencia prioridad = PrioridadIncidencia.NORMAL;
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;
    @PrePersist
    protected void onCreate() {
        creadoEn = OffsetDateTime.now();
    }
}
