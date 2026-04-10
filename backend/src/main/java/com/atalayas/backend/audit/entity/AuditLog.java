package com.atalayas.backend.audit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Registro de auditoría de eventos relevantes de la plataforma.
 * Alimenta "actividadReciente" del dashboard del superadmin.
 * Tipos válidos: "info" | "success" | "warning" | "error"
 */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "texto", nullable = false, length = 500)
    private String texto;

    /** info | success | warning | error */
    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @PrePersist
    protected void onCreate() {
        creadoEn = OffsetDateTime.now();
    }
}

