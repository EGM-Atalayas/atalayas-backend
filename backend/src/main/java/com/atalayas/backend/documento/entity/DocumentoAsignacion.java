package com.atalayas.backend.documento.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad mapeada a la tabla 'documento_asignacion'.
 *
 * Relación N:M entre {@link Documento} y usuarios. Cada fila indica
 * que un empleado tiene asignado un documento, junto con el estado
 * (visto / firmado) y la URL del PDF firmado si procede.
 */
@Entity
@Table(name = "documento_asignacion",
        uniqueConstraints = @UniqueConstraint(columnNames = {"documento_id", "usuario_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoAsignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "asignacion_id", updatable = false, nullable = false)
    private UUID asignacionId;

    @Column(name = "documento_id", nullable = false)
    private UUID documentoId;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "fecha_asignacion", updatable = false)
    private OffsetDateTime fechaAsignacion;

    @Column(name = "visto", nullable = false)
    @Builder.Default
    private boolean visto = false;

    @Column(name = "fecha_visto")
    private OffsetDateTime fechaVisto;

    @Column(name = "firmado", nullable = false)
    @Builder.Default
    private boolean firmado = false;

    @Column(name = "fecha_firma")
    private OffsetDateTime fechaFirma;

    /** URL del PDF con la firma estampada (fase 3) */
    @Column(name = "firma_url", length = 500)
    private String firmaUrl;

    @PrePersist
    protected void onCreate() {
        fechaAsignacion = OffsetDateTime.now();
    }
}
