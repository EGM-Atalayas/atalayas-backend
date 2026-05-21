package com.atalayas.backend.documento.entity;

import com.atalayas.backend.documento.enums.TipoDocumento;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad mapeada a la tabla 'documento'.
 *
 * Representa un documento subido por un administrador y asignado a uno
 * o varios empleados (relación a través de {@link DocumentoAsignacion}).
 */
@Entity
@Table(name = "documento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "documento_id", updatable = false, nullable = false)
    private UUID documentoId;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoDocumento tipo;

    @Column(name = "archivo_url", nullable = false, length = 500)
    private String archivoUrl;

    @Column(name = "archivo_nombre", nullable = false, length = 255)
    private String archivoNombre;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    @Column(name = "subido_por", nullable = false)
    private UUID subidoPor;

    @Column(name = "requiere_firma", nullable = false)
    @Builder.Default
    private boolean requiereFirma = false;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(name = "fecha_subida", updatable = false)
    private OffsetDateTime fechaSubida;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @PrePersist
    protected void onCreate() {
        fechaSubida = OffsetDateTime.now();
        actualizadoEn = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = OffsetDateTime.now();
    }
}
