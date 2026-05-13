package com.atalayas.backend.common.service;

import com.atalayas.backend.communication.client.MailerooClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetEmailService {

    private final MailerooClient mailerooClient;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Async
    public void enviarRecuperacionPassword(String destinatario, String nombre, String token) {
        String enlace = frontendUrl + "/reset-password?token=" + token;

        String html = """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8"/></head>
                <body style="margin:0;padding:0;background:#f5f6f8;font-family:'Segoe UI',Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f5f6f8;padding:40px 0;">
                    <tr><td align="center">
                      <table width="560" cellpadding="0" cellspacing="0"
                             style="background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);">
                        <tr>
                          <td style="background:#1B3F7E;padding:32px 40px;text-align:center;">
                            <h1 style="color:#ffffff;margin:0;font-size:22px;font-weight:700;letter-spacing:-0.5px;">
                              Atalayas Área Empresarial
                            </h1>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:40px 40px 32px;">
                            <p style="color:#0f1923;font-size:16px;margin:0 0 8px;">Hola, <strong>%s</strong></p>
                            <p style="color:#3D4A5C;font-size:15px;line-height:1.6;margin:0 0 28px;">
                              Hemos recibido una solicitud para restablecer la contraseña de tu cuenta.
                              Haz clic en el botón de abajo para crear una nueva contraseña.
                            </p>
                            <table cellpadding="0" cellspacing="0" width="100%%">
                              <tr><td align="center">
                                <a href="%s"
                                   style="display:inline-block;background:#1B3F7E;color:#ffffff;
                                          text-decoration:none;font-weight:600;font-size:15px;
                                          padding:14px 36px;border-radius:10px;letter-spacing:0.2px;">
                                  Restablecer contraseña
                                </a>
                              </td></tr>
                            </table>
                            <p style="color:#6B7A8D;font-size:13px;margin:28px 0 0;line-height:1.6;">
                              Este enlace expirará en <strong>30 minutos</strong>. Si no solicitaste
                              restablecer tu contraseña, ignora este correo.
                            </p>
                          </td>
                        </tr>
                        <tr>
                          <td style="background:#f5f6f8;padding:20px 40px;text-align:center;
                                     border-top:1px solid #e2e5ea;">
                            <p style="color:#9BA3B0;font-size:12px;margin:0;">
                              © 2026 Atalayas Área Empresarial · Alicante
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(nombre, enlace);

        try {
            mailerooClient.send(from, destinatario, "Restablecer contraseña · Atalayas", html);
            log.info("[MailerooClient] Correo de recuperación enviado a {}", destinatario);
        } catch (Exception e) {
            log.error("[MailerooClient] Error al enviar correo a {}: {}", destinatario, e.getMessage());
        }
    }
}
