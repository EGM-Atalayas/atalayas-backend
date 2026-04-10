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