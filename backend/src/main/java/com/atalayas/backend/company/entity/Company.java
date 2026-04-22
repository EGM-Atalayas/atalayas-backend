package com.atalayas.backend.company.entity;

import com.atalayas.backend.common.enums.EstadoSolicitud;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad mapeada a la tabla 'empresa' de PostgreSQL.
 *
 * Una empresa representa a una organización del parque empresarial EGM.
 * Puede estar en estado PENDIENTE, APROBADA o PAUSADA según el proceso
 * de validación del superadmin. Las empresas rechazadas se eliminan físicamente
 * de la BD. Solo las empresas APROBADAS y activas tienen acceso completo.
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

    // URL del logo — se muestra en el header cuando el usuario es admin empresa
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "email_contacto", nullable = false, length = 255)
    private String emailContacto;

    @Column(name = "telefono_contacto", length = 20)
    private String telefonoContacto;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    // Datos de identidad corporativa — se usan en el módulo de onboarding
    @Column(name = "mision", columnDefinition = "TEXT")
    private String mision;

    @Column(name = "vision", columnDefinition = "TEXT")
    private String vision;

    @Column(name = "valores", columnDefinition = "TEXT")
    private String valores;

    // Estado del proceso de alta en la plataforma
    // PENDIENTE → esperando revisión de EGM
    // APROBADA  → acceso completo habilitado
    // PAUSADA   → suspendida temporalmente por el superadmin
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_solicitud", nullable = false, length = 20)
    @Builder.Default
    private EstadoSolicitud estadoSolicitud = EstadoSolicitud.PENDIENTE;

    // true si la empresa es la propia EGM — tiene permisos especiales de comunicación
    @Column(name = "es_egm", nullable = false)
    @Builder.Default
    private boolean esEgm = false;

    // Soft delete — false significa que la empresa está suspendida o rechazada
    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    // Fecha en que se envió la solicitud de alta — no se modifica nunca
    @Column(name = "fecha_solicitud", updatable = false, nullable = false)
    private OffsetDateTime fechaSolicitud;

    // Fecha en que el superadmin tomó la decisión de aprobar o rechazar
    @Column(name = "fecha_resolucion")
    private OffsetDateTime fechaResolucion;

    // Gestionado automáticamente — no asignar manualmente en updates
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @PrePersist
    protected void onCreate() {
        fechaSolicitud = OffsetDateTime.now();
        actualizadoEn = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = OffsetDateTime.now();
    }
}