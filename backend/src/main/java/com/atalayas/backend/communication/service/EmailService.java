package com.atalayas.backend.communication.service;

import com.atalayas.backend.communication.client.MailerooClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Servicio de envío de correo electrónico.
 *
 * Todos los métodos públicos son @Async: fire-and-forget, no bloquean el hilo HTTP.
 * Los errores se capturan internamente y se registran en log ERROR.
 *
 * Métodos individuales  — un destinatario  → mailerooClient.send()
 * Métodos masivos (_Masivo) — BCC batch     → mailerooClient.sendBcc()
 *                             Cap: app.mail.max-bulk-recipients (defecto 25,
 *                             respeta límite de 30 emails/hora del plan Free)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final MailerooClient mailerooClient;

    @Value("${app.mail.from}")
    private String remitente;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    /** Máximo de destinatarios BCC por envío. Plan Free Maileroo: 30 emails/hora. */
    @Value("${app.mail.max-bulk-recipients:25}")
    private int maxBulkRecipients;

    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── INDIVIDUALES ─────────────────────────────────────────────────────────

    @Async
    public void enviarAprobacion(String emailDestino, String nombre, String nombreEmpresa) {
        try {
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
        } catch (Exception e) {
            log.error("[EmailService] Error al enviar aprobación a {}: {}", emailDestino, e.getMessage());
        }
    }

    @Async
    public void enviarRechazo(String emailDestino, String nombre, String nombreEmpresa) {
        try {
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
        } catch (Exception e) {
            log.error("[EmailService] Error al enviar rechazo a {}: {}", emailDestino, e.getMessage());
        }
    }

    @Async
    public void enviarBienvenidaUsuarioCreado(String emailDestino, String nombre, String nombreEmpresa) {
        try {
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
        } catch (Exception e) {
            log.error("[EmailService] Error al enviar bienvenida a {}: {}", emailDestino, e.getMessage());
        }
    }

    // ── BULK BCC — EVENTOS EGM ────────────────────────────────────────────────

    /**
     * Notifica a todos los usuarios activos de la plataforma que hay un nuevo evento EGM.
     * BCC: 1 llamada API, N destinatarios. Cap: app.mail.max-bulk-recipients.
     */
    @Async
    public void enviarNuevoEventoMasivo(List<String> emails, String titulo, LocalDate fecha, String lugar) {
        if (emails == null || emails.isEmpty()) return;
        try {
            List<String> dest = capRecipients(emails, "NuevoEvento[" + titulo + "]");
            String lugarHtml = (lugar != null && !lugar.isBlank())
                    ? "<p style='color:#3D4A5C;font-size:14px;margin:4px 0 0;'>📍 " + lugar + "</p>"
                    : "";
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
                                  <span style="display:inline-block;background:#FFF7ED;color:#EA580C;
                                               font-size:13px;font-weight:700;padding:6px 14px;
                                               border-radius:20px;letter-spacing:0.3px;">
                                    📅 Nuevo evento
                                  </span>
                                </p>
                                <p style="color:#3D4A5C;font-size:15px;line-height:1.6;margin:0 0 20px;">
                                  Hay un nuevo evento disponible en la plataforma. ¡No te lo pierdas!
                                </p>
                                <table cellpadding="0" cellspacing="0" width="100%%"
                                       style="margin:0 0 28px;">
                                  <tr>
                                    <td style="background:#f5f6f8;border-radius:10px;padding:20px 24px;">
                                      <p style="color:#0f1923;font-size:17px;font-weight:700;margin:0 0 6px;">%s</p>
                                      <p style="color:#3D4A5C;font-size:14px;margin:0;">📆 %s</p>
                                      %s
                                    </td>
                                  </tr>
                                </table>
                                <table cellpadding="0" cellspacing="0" width="100%%">
                                  <tr><td align="center">
                                    <a href="%s/eventos"
                                       style="display:inline-block;background:#1B3F7E;color:#ffffff;
                                              text-decoration:none;font-weight:600;font-size:15px;
                                              padding:14px 36px;border-radius:10px;letter-spacing:0.2px;">
                                      Ver todos los eventos
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
                    """.formatted(titulo, fecha.format(DATE_FMT), lugarHtml, frontendUrl);
            sendBcc(dest, "Nuevo evento · " + titulo, html);
        } catch (Exception e) {
            log.error("[EmailService] Error al enviar nuevo evento masivo '{}': {}", titulo, e.getMessage());
        }
    }

    /**
     * Notifica a todos los usuarios activos de la plataforma que un evento EGM ha sido cancelado.
     */
    @Async
    public void enviarEventoCanceladoMasivo(List<String> emails, String titulo, LocalDate fecha) {
        if (emails == null || emails.isEmpty()) return;
        try {
            List<String> dest = capRecipients(emails, "EventoCancelado[" + titulo + "]");
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
                                  <span style="display:inline-block;background:#F3F4F6;color:#6B7280;
                                               font-size:13px;font-weight:700;padding:6px 14px;
                                               border-radius:20px;letter-spacing:0.3px;">
                                    ❌ Evento cancelado
                                  </span>
                                </p>
                                <p style="color:#3D4A5C;font-size:15px;line-height:1.6;margin:0 0 20px;">
                                  El siguiente evento ha sido cancelado:
                                </p>
                                <table cellpadding="0" cellspacing="0" width="100%%"
                                       style="margin:0 0 28px;">
                                  <tr>
                                    <td style="background:#f5f6f8;border-radius:10px;padding:20px 24px;">
                                      <p style="color:#0f1923;font-size:17px;font-weight:700;margin:0 0 6px;">%s</p>
                                      <p style="color:#3D4A5C;font-size:14px;margin:0;">📆 %s</p>
                                    </td>
                                  </tr>
                                </table>
                                <p style="color:#6B7A8D;font-size:13px;margin:0;line-height:1.6;">
                                  Consulta la plataforma para ver los próximos eventos disponibles.
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
                    """.formatted(titulo, fecha.format(DATE_FMT));
            sendBcc(dest, "Evento cancelado · " + titulo, html);
        } catch (Exception e) {
            log.error("[EmailService] Error al enviar cancelación masiva '{}': {}", titulo, e.getMessage());
        }
    }

    // ── BULK BCC — EVENTOS COMUNIDAD ──────────────────────────────────────────

    /**
     * Notifica a los usuarios del scope del evento (empresa o global) que hay un nuevo evento de comunidad.
     */
    @Async
    public void enviarNuevoEventoComunidadMasivo(List<String> emails, String titulo,
                                                  OffsetDateTime fechaInicio, String lugar) {
        if (emails == null || emails.isEmpty()) return;
        try {
            List<String> dest = capRecipients(emails, "NuevoEventoComunidad[" + titulo + "]");
            String lugarHtml = (lugar != null && !lugar.isBlank())
                    ? "<p style='color:#3D4A5C;font-size:14px;margin:4px 0 0;'>📍 " + lugar + "</p>"
                    : "";
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
                                  <span style="display:inline-block;background:#F5F3FF;color:#7C3AED;
                                               font-size:13px;font-weight:700;padding:6px 14px;
                                               border-radius:20px;letter-spacing:0.3px;">
                                    🎉 Evento de comunidad
                                  </span>
                                </p>
                                <p style="color:#3D4A5C;font-size:15px;line-height:1.6;margin:0 0 20px;">
                                  Se ha publicado un nuevo evento de comunidad para ti:
                                </p>
                                <table cellpadding="0" cellspacing="0" width="100%%"
                                       style="margin:0 0 28px;">
                                  <tr>
                                    <td style="background:#f5f6f8;border-radius:10px;padding:20px 24px;">
                                      <p style="color:#0f1923;font-size:17px;font-weight:700;margin:0 0 6px;">%s</p>
                                      <p style="color:#3D4A5C;font-size:14px;margin:0;">📆 %s</p>
                                      %s
                                    </td>
                                  </tr>
                                </table>
                                <table cellpadding="0" cellspacing="0" width="100%%">
                                  <tr><td align="center">
                                    <a href="%s/comunidad"
                                       style="display:inline-block;background:#7C3AED;color:#ffffff;
                                              text-decoration:none;font-weight:600;font-size:15px;
                                              padding:14px 36px;border-radius:10px;letter-spacing:0.2px;">
                                      Ver evento
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
                    """.formatted(titulo, fechaInicio.format(DATETIME_FMT), lugarHtml, frontendUrl);
            sendBcc(dest, "Nuevo evento de comunidad · " + titulo, html);
        } catch (Exception e) {
            log.error("[EmailService] Error al enviar nuevo evento comunidad masivo '{}': {}", titulo, e.getMessage());
        }
    }

    /**
     * Notifica a los usuarios del scope del evento que un evento de comunidad ha sido desactivado.
     */
    @Async
    public void enviarEventoComunidadDesactivadoMasivo(List<String> emails, String titulo) {
        if (emails == null || emails.isEmpty()) return;
        try {
            List<String> dest = capRecipients(emails, "EventoComunidadDesactivado[" + titulo + "]");
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
                                  <span style="display:inline-block;background:#F3F4F6;color:#6B7280;
                                               font-size:13px;font-weight:700;padding:6px 14px;
                                               border-radius:20px;letter-spacing:0.3px;">
                                    ❌ Evento desactivado
                                  </span>
                                </p>
                                <p style="color:#3D4A5C;font-size:15px;line-height:1.6;margin:0 0 20px;">
                                  El siguiente evento de comunidad ha sido desactivado:
                                </p>
                                <table cellpadding="0" cellspacing="0" width="100%%"
                                       style="margin:0 0 28px;">
                                  <tr>
                                    <td style="background:#f5f6f8;border-radius:10px;padding:20px 24px;">
                                      <p style="color:#0f1923;font-size:17px;font-weight:700;margin:0;">%s</p>
                                    </td>
                                  </tr>
                                </table>
                                <p style="color:#6B7A8D;font-size:13px;margin:0;line-height:1.6;">
                                  Consulta la sección de comunidad para ver los eventos disponibles.
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
                    """.formatted(titulo);
            sendBcc(dest, "Evento desactivado · " + titulo, html);
        } catch (Exception e) {
            log.error("[EmailService] Error al enviar desactivación comunidad masiva '{}': {}", titulo, e.getMessage());
        }
    }

    // ── PRIVADOS ──────────────────────────────────────────────────────────────

    /**
     * Trunca la lista al cap configurado y emite WARN si se recorta.
     * 30 emails/hora (plan Free) → cap 25 deja margen para envíos individuales.
     */
    private List<String> capRecipients(List<String> emails, String context) {
        if (emails.size() > maxBulkRecipients) {
            log.warn("[EmailService] {} — lista de {} destinatarios truncada a {} (límite plan Maileroo Free 30/h)",
                    context, emails.size(), maxBulkRecipients);
            return emails.subList(0, maxBulkRecipients);
        }
        return emails;
    }

    private void send(String to, String subject, String html) {
        mailerooClient.send(remitente, to, subject, html);
    }

    private void sendBcc(List<String> bccList, String subject, String html) {
        mailerooClient.sendBcc(remitente, subject, html, bccList);
    }
}
