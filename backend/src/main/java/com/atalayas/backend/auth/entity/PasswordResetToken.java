package com.atalayas.backend.auth.entity;

import com.atalayas.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Token de un solo uso para restablecer la contraseña.
 * Expira a los 30 minutos de su creación.
 */
@Entity
@Table(name = "password_reset_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private OffsetDateTime expiraEn;

    @Column(nullable = false)
    @Builder.Default
    private boolean usado = false;

    public boolean isExpirado() {
        return OffsetDateTime.now().isAfter(expiraEn);
    }
}
