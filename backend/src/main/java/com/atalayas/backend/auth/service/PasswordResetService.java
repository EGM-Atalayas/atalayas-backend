package com.atalayas.backend.auth.service;

import com.atalayas.backend.auth.entity.PasswordResetToken;
import com.atalayas.backend.auth.repository.PasswordResetTokenRepository;
import com.atalayas.backend.common.service.EmailService;
import com.atalayas.backend.user.entity.User;
import com.atalayas.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    private static final int EXPIRACION_MINUTOS = 30;

    /**
     * Solicitud de recuperación: genera token y envía el correo.
     * Siempre devuelve OK para no revelar si el email existe.
     */
    @Transactional
    public void solicitarRecuperacion(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            // Invalida tokens anteriores del mismo usuario
            tokenRepository.deleteByUsuarioId(user.getUsuarioId());

            // Genera token seguro
            String rawToken = generarToken();

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(rawToken)
                    .user(user)
                    .expiraEn(OffsetDateTime.now().plusMinutes(EXPIRACION_MINUTOS))
                    .usado(false)
                    .build();

            tokenRepository.save(resetToken);

            // Envía el correo de forma asíncrona
            emailService.enviarRecuperacionPassword(user.getEmail(), user.getNombre(), rawToken);
        });
    }

    /**
     * Restablece la contraseña usando el token recibido por email.
     */
    @Transactional
    public void restablecerPassword(String rawToken, String nuevaPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido o expirado"));

        if (resetToken.isUsado()) {
            throw new IllegalArgumentException("Este enlace ya ha sido utilizado");
        }

        if (resetToken.isExpirado()) {
            throw new IllegalArgumentException("El enlace ha expirado. Solicita uno nuevo");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(nuevaPassword));
        // Reinicia intentos fallidos por si la cuenta estaba bloqueada
        user.setIntentosFallidos(0);
        userRepository.save(user);

        // Marca el token como usado
        resetToken.setUsado(true);
        tokenRepository.save(resetToken);
    }

    private String generarToken() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
