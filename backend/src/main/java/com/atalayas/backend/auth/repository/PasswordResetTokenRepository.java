package com.atalayas.backend.auth.repository;

import com.atalayas.backend.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByToken(String token);

    /** Elimina todos los tokens caducados — se puede llamar periódicamente */
    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiraEn < :ahora")
    void deleteExpired(OffsetDateTime ahora);

    /** Invalida todos los tokens anteriores del mismo usuario antes de crear uno nuevo */
    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken t WHERE t.user.usuarioId = :usuarioId")
    void deleteByUsuarioId(UUID usuarioId);
}
