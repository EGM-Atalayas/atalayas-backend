package com.atalayas.backend.communication.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Servicio de envío de correo electrónico en texto plano.
 * Gestiona las notificaciones del ciclo de vida de solicitudes de empresa.
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String remitente;

    /**
     * Notifica al administrador de empresa que su solicitud ha sido APROBADA.
     * A partir de este momento su cuenta queda activa y puede iniciar sesión.
     *
     * @param emailDestino email del usuario admin de la empresa
     * @param nombre       nombre del usuario admin
     * @param nombreEmpresa nombre de la empresa aprobada
     */
    public void enviarAprobacion(String emailDestino, String nombre, String nombreEmpresa) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(emailDestino);
        mensaje.setSubject("¡Tu empresa ha sido aprobada en Atalayas!");
        mensaje.setText(
                "Hola " + nombre + ",\n\n" +
                "Nos complace informarte que la solicitud de alta para la empresa \"" + nombreEmpresa + "\" " +
                "ha sido APROBADA por nuestro equipo.\n\n" +
                "Tu cuenta ya está activa. Puedes iniciar sesión en la plataforma con el email y contraseña " +
                "que registraste durante la solicitud.\n\n" +
                "Si tienes cualquier duda, no dudes en contactarnos.\n\n" +
                "Un saludo,\n" +
                "El equipo de Atalayas"
        );
        mailSender.send(mensaje);
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
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(emailDestino);
        mensaje.setSubject("Actualización sobre tu solicitud de alta en Atalayas");
        mensaje.setText(
                "Hola " + nombre + ",\n\n" +
                "Lamentamos informarte que la solicitud de alta para la empresa \"" + nombreEmpresa + "\" " +
                "no ha podido ser aprobada en este momento.\n\n" +
                "Si crees que esto es un error o deseas más información, ponte en contacto con " +
                "nuestro equipo de soporte respondiendo a este correo.\n\n" +
                "Un saludo,\n" +
                "El equipo de Atalayas"
        );
        mailSender.send(mensaje);
    }
}

