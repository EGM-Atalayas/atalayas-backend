package com.atalayas.backend.communication.service;

import com.atalayas.backend.communication.client.MailerooClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Servicio de envío de correo electrónico en texto plano.
 * Gestiona las notificaciones del ciclo de vida de solicitudes de empresa.
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private final MailerooClient mailerooClient;

    @Value("${app.mail.from}")
    private String remitente;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * Notifica al administrador de empresa que su solicitud ha sido APROBADA.
     * A partir de este momento su cuenta queda activa y puede iniciar sesión.
     *
     * @param emailDestino email del usuario admin de la empresa
     * @param nombre       nombre del usuario admin
     * @param nombreEmpresa nombre de la empresa aprobada
     */
    public void enviarAprobacion(String emailDestino, String nombre, String nombreEmpresa) {
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
                            <p style="margin:0 0 20px;">
                              <span style="display:inline-block;background:#ECFDF5;color:#059669;
                                           font-size:13px;font-weight:700;padding:6px 14px;
                                           border-radius:20px;letter-spacing:0.3px;">
                                ✓ Solicitud aprobada
                              </span>
                            </p>
                            <p style="color:#0f1923;font-size:16px;margin:0 0 8px;">
                              Hola, <strong>%s</strong>
                            </p>
                            <p style="color:#3D4A5C;font-size:15px;line-height:1.6;margin:0 0 12px;">
                              Nos complace informarte que la solicitud de alta para la empresa
                              <strong>%s</strong> ha sido <strong>aprobada</strong> por nuestro equipo.
                            </p>
                            <p style="color:#3D4A5C;font-size:15px;line-height:1.6;margin:0 0 28px;">
                              Tu cuenta ya está activa. Puedes acceder a la plataforma con el email
                              y contraseña que registraste durante la solicitud.
                            </p>
                            <table cellpadding="0" cellspacing="0" width="100%%">
                              <tr><td align="center">
                                <a href="%s/login"
                                   style="display:inline-block;background:#1B3F7E;color:#ffffff;
                                          text-decoration:none;font-weight:600;font-size:15px;
                                          padding:14px 36px;border-radius:10px;letter-spacing:0.2px;">
                                  Acceder a la plataforma
                                </a>
                              </td></tr>
                            </table>
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
                """.formatted(nombre, nombreEmpresa, frontendUrl);
        send(emailDestino, "¡Tu empresa ha sido aprobada en Atalayas!", html);
    }

    /**
     * Notifica al administrador de empresa que su solicitud ha sido RECHAZADA.
     * La cuenta permanece inactiva en el sistema.
     *
     * @param emailDestino  email del usuario admin de la empresa
     * @param nombre        nombre del usuario admin
     * @param nombreEmpresa nombre de la empresa rechazada
     */
    public void enviarRechazo(String emailDestino, String nombre, String nombreEmpresa) {
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
                            <p style="margin:0 0 20px;">
                              <span style="display:inline-block;background:#FEF2F2;color:#DC2626;
                                           font-size:13px;font-weight:700;padding:6px 14px;
                                           border-radius:20px;letter-spacing:0.3px;">
                                ✗ Solicitud no aprobada
                              </span>
                            </p>
                            <p style="color:#0f1923;font-size:16px;margin:0 0 8px;">
                              Hola, <strong>%s</strong>
                            </p>
                            <p style="color:#3D4A5C;font-size:15px;line-height:1.6;margin:0 0 12px;">
                              Lamentamos informarte que la solicitud de alta para la empresa
                              <strong>%s</strong> no ha podido ser aprobada en este momento.
                            </p>
                            <p style="color:#3D4A5C;font-size:15px;line-height:1.6;margin:0 0 28px;">
                              Si crees que esto es un error o necesitas más información, no dudes
                              en ponerte en contacto con nuestro equipo de soporte:
                            </p>
                            <p style="text-align:center;margin:0 0 28px;">
                              <a href="mailto:soporte@atalayas.com"
                                 style="color:#1B3F7E;font-weight:600;font-size:15px;">
                                soporte@atalayas.com
                              </a>
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
                """.formatted(nombre, nombreEmpresa);
        send(emailDestino, "Actualización sobre tu solicitud de alta en Atalayas", html);
    }

    /**
     * Notifica al usuario recién creado por un admin que su cuenta ya está lista.
     *
     * @param emailDestino  email del nuevo usuario
     * @param nombre        nombre del nuevo usuario
     * @param nombreEmpresa nombre de la empresa a la que pertenece
     */
    public void enviarBienvenidaUsuarioCreado(String emailDestino, String nombre, String nombreEmpresa) {
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
                            <p style="margin:0 0 20px;">
                              <span style="display:inline-block;background:#EFF6FF;color:#1B3F7E;
                                           font-size:13px;font-weight:700;padding:6px 14px;
                                           border-radius:20px;letter-spacing:0.3px;">
                                Tu cuenta está lista
                              </span>
                            </p>
                            <p style="color:#0f1923;font-size:16px;margin:0 0 8px;">
                              Hola, <strong>%s</strong>
                            </p>
                            <p style="color:#3D4A5C;font-size:15px;line-height:1.6;margin:0 0 12px;">
                              Un administrador de <strong>%s</strong> ha creado tu cuenta en la
                              plataforma Atalayas. Puedes iniciar sesión con las siguientes credenciales:
                            </p>
                            <table cellpadding="0" cellspacing="0" width="100%%"
                                   style="margin:0 0 24px;">
                              <tr>
                                <td style="background:#f5f6f8;border-radius:10px;padding:16px 20px;">
                                  <p style="margin:0 0 4px;color:#6B7A8D;font-size:12px;font-weight:600;
                                             text-transform:uppercase;letter-spacing:0.5px;">Email</p>
                                  <code style="color:#1B3F7E;font-size:15px;font-weight:700;">%s</code>
                                </td>
                              </tr>
                            </table>
                            <p style="color:#3D4A5C;font-size:15px;line-height:1.6;margin:0 0 28px;">
                              La contraseña inicial te la ha comunicado tu administrador.
                              Te recomendamos cambiarla tras el primer inicio de sesión.
                            </p>
                            <table cellpadding="0" cellspacing="0" width="100%%">
                              <tr><td align="center">
                                <a href="%s/login"
                                   style="display:inline-block;background:#1B3F7E;color:#ffffff;
                                          text-decoration:none;font-weight:600;font-size:15px;
                                          padding:14px 36px;border-radius:10px;letter-spacing:0.2px;">
                                  Iniciar sesión
                                </a>
                              </td></tr>
                            </table>
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
                """.formatted(nombre, nombreEmpresa, emailDestino, frontendUrl);
        send(emailDestino, "Tu cuenta en Atalayas está lista", html);
    }

    private void send(String to, String subject, String html) {
        mailerooClient.send(remitente, to, subject, html);
    }
}
