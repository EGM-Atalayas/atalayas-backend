package com.atalayas.backend.user.entity;

import com.atalayas.backend.role.entity.Rol;
import com.atalayas.backend.department.entity.Departamento;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Entidad mapeada a la tabla 'usuario'
 *
 * El email actúa como username de autenticación
 * El rol determina qué puede ver y hacer el usuario en la plataforma
 */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "usuario_id", updatable = false, nullable = false)
    private UUID usuarioId;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String password;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "banner_url", length = 500)
    private String bannerUrl;

    @Column(name = "bio", length = 300)
    private String bio;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(name = "disponibilidad", length = 20)
    @Builder.Default
    private com.atalayas.backend.user.enums.Disponibilidad disponibilidad = com.atalayas.backend.user.enums.Disponibilidad.DISPONIBLE;

    @Column(name = "notif_nuevo_modulo", nullable = false)
    @Builder.Default
    private boolean notifNuevoModulo = true;

    @Column(name = "notif_modulo_completado", nullable = false)
    @Builder.Default
    private boolean notifModuloCompletado = true;

    @Column(name = "notif_comunicado", nullable = false)
    @Builder.Default
    private boolean notifComunicado = true;

    @Column(name = "notif_pendiente", nullable = false)
    @Builder.Default
    private boolean notifPendiente = true;

    @Column(name = "modo_oscuro", nullable = false)
    @Builder.Default
    private boolean modoOscuro = false;

    // FK a la empresa a la que pertenece el usuario
    @Column(name = "empresa_id")
    private UUID empresaId;

    // FK al rol
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    // Contador de intentos fallidos de login bloquea la cuenta a partir de 5
    @Column(name = "intentos_fallidos", nullable = false)
    @Builder.Default
    private int intentosFallidos = 0;

    @Column(name = "terminos_aceptados", nullable = false)
    @Builder.Default
    private boolean terminosAceptados = false;

    // Puesto de trabajo visible en el header debajo del nombre
    @Column(name = "puesto_trabajo", length = 150)
    private String puestoTrabajo;

    // Departamento al que pertenece el empleado (usado para visibilidad de módulos)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departamento_id")
    private Departamento departamento;

    // Soft delete, false significa que la cuenta está desactivada
    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(name = "fecha_registro", updatable = false)
    private OffsetDateTime fechaRegistro;

    // Fecha en la que el usuario fue dado de baja (activo = false)
    // null si el usuario sigue activo
    @Column(name = "fecha_baja")
    private OffsetDateTime fechaBaja;

    @Column(name = "ultimo_login")
    private OffsetDateTime ultimoLogin;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @PrePersist
    protected void onCreate() {
        fechaRegistro = OffsetDateTime.now();
        actualizadoEn = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = OffsetDateTime.now();
    }


    // ── UserDetails, implementación requerida por Spring Security ─────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(rol.getCodigoRol()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }

    @Override
    public boolean isAccountNonLocked() {
        // La cuenta se bloquea automáticamente tras 5 intentos fallidos
        return intentosFallidos < 5;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // Nombre completo para mostrar en notificaciones y comunicados
    public String getNombreCompleto() {
        return nombre + " " + apellidos;
    }
}
