package com.atalayas.backend.company.entity;

import com.atalayas.backend.common.enums.EstadoSolicitud;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad mapeada a la tabla 'empresa' de la BBDD
 *
 * Una empresa representa a una organización de EGM.
 * Puede estar en estado PENDIENTE (esperando aprobación), APROBADA o RECHAZADA.
 * Solo las empresas APROBADAS y activas tienen acceso completo a la plataforma.
 */
@Entity
@Table(name = "empresa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "empresa_id", updatable = false, nullable = false)
    private UUID empresaId;

    @Column(name = "nombre_empresa", nullable = false, length = 200)
    private String nombreEmpresa;

    @Column(name = "cif", nullable = false, unique = true, length = 20)
    private String cif;

    @Column(name = "sector", length = 100)
    private String sector;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "email_contacto", nullable = false, length = 255)
    private String emailContacto;

    @Column(name = "telefono_contacto", length = 20)
    private String telefonoContacto;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "mision", columnDefinition = "TEXT")
    private String mision;

    @Column(name = "vision", columnDefinition = "TEXT")
    private String vision;

    @Column(name = "valores", columnDefinition = "TEXT")
    private String valores;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_solicitud", nullable = false, length = 20)
    @Builder.Default
    private EstadoSolicitud estadoSolicitud = EstadoSolicitud.PENDIENTE;

    // Indica si la empresa pertenece al propio EGM (empresa gestora del parque)
    @Column(name = "es_egm", nullable = false)
    @Builder.Default
    private boolean esEgm = false;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(name = "fecha_solicitud", updatable = false, nullable = false)
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @PrePersist
    protected void onCreate() {
        fechaSolicitud = LocalDateTime.now();
        actualizadoEn = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = LocalDateTime.now();
    }
}