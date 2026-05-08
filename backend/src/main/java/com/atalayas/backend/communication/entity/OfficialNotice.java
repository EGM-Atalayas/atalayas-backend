package com.atalayas.backend.communication.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad mapeada a la tabla 'comunicado'
 *
 * Representa un comunicado oficial publicado por EGM Atalayas
 * dirigido a todos los usuarios de la plataforma o a un subconjunto
 *
 * Solo ROLE_ADMIN puede crear y desactivar comunicados oficiales
 * Todos los usuarios autenticados pueden leerlos
 */
@Entity
@Table(name = "comunicado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfficialNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "comunicado_id", updatable = false, nullable = false)
    private UUID comunicadoId;

    // ID del superadmin que publicó el comunicado
    @Column(name = "creado_por")
    private UUID creadoPor;

    @Column(name = "titulo", nullable = false, length = 250)
    private String titulo;

    @Column(name = "mensaje", nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    // URL de imagen opcional
    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    // Categoría visible en el feed (General, Novedad, Aviso, Evento)
    @Column(name = "categoria", length = 50)
    private String categoria;

    // Si true aparece fijado al principio del listado
    @Column(name = "destacado", nullable = false)
    @Builder.Default
    private boolean destacado = false;

    // 'publicado' | 'borrador' — los borradores solo los ve el superadmin
    @Column(name = "estado", length = 20)
    @Builder.Default
    private String estado = "publicado";

    // Recursos opcionales
    @Column(name = "enlace_url", length = 500)
    private String enlaceUrl;

    @Column(name = "enlace_texto", length = 200)
    private String enlaceTexto;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(name = "adjunto_url", length = 500)
    private String adjuntoUrl;

    @Column(name = "adjunto_nombre", length = 200)
    private String adjuntoNombre;

    /**
     * Fecha de publicación del comunicado.
     * Permite programar comunicados para una fecha futura enviando
     * una fecha posterior a la actual.
     */
    @Column(name = "fecha_publicacion")
    private OffsetDateTime fechaPublicacion;

    /**
     * Fecha de expiración opcional.
     * Si es null, el comunicado no caduca automáticamente.
     */
    @Column(name = "fecha_expiracion")
    private OffsetDateTime fechaExpiracion;

    // Soft delete, permite retirar un comunicado de forma inmediata
    // sin perder el historial ni afectar a la trazabilidad
    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    // Gestionado por trigger en BD
    @Column(name = "actualizado_en")
    private OffsetDateTime actualizadoEn;

    // Si no viene fecha de publicación en el request, usamos now()
    @PrePersist
    protected void onCreate() {
        if (fechaPublicacion == null) {
            fechaPublicacion = OffsetDateTime.now();
        }
    }
}