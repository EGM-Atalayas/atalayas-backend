package com.atalayas.backend.communication.client;

import com.atalayas.backend.exception.EmailSendException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Cliente HTTP hacia la API REST de Maileroo.
 * Usa java.net.http.HttpClient nativo (Java 21), sin dependencias externas.
 *
 * Variables de entorno requeridas:
 *   MAILEROO_API_KEY — clave de API obtenida en https://maileroo.com
 *   MAILEROO_API_URL — (opcional) endpoint; por defecto https://api.maileroo.com/send
 */
@Slf4j
@Component
public class MailerooClient {

    @Value("${maileroo.api.key}")
    private String apiKey;

    @Value("${maileroo.api.url:https://api.maileroo.com/send}")
    private String apiUrl;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public MailerooClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Envía un email HTML mediante la API de Maileroo.
     *
     * @param from    dirección remitente (debe estar verificada en Maileroo)
     * @param to      dirección destinataria
     * @param subject asunto del correo
     * @param html    cuerpo HTML del correo
     * @throws EmailSendException si la API devuelve un error HTTP o falla la conexión
     */
    public void send(String from, String to, String subject, String html) {
        try {
            Map<String, String> body = Map.of(
                    "from", from,
                    "to", to,
                    "subject", subject,
                    "html", html
            );
            String bodyJson = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("X-API-Key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("[MailerooClient] Error HTTP {} al enviar a {}: {}", response.statusCode(), to, response.body());
                throw new EmailSendException(
                        "Maileroo devolvió HTTP " + response.statusCode() + " al enviar a " + to, null);
            }

            log.debug("[MailerooClient] Email enviado a {} — HTTP {}", to, response.statusCode());

        } catch (EmailSendException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new EmailSendException("Error de conexión con Maileroo al enviar a " + to + ": " + ex.getMessage(), ex);
        }
    }
}

