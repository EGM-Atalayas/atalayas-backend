package com.atalayas.backend.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Cliente HTTP hacia la API de Google Gemini 2.0 Flash
 * Punto único de comunicación con la IA - todos los servicios pasan por aquí
 * Usa java.net.http.HttpClient nativo, sin dependencias extra
 */
@Slf4j
@Component
public class GeminiClient {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Value("${gemini.max-tokens}")
    private int maxTokens;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }


    /**
     * Envía un prompt a Gemini y devuelve el texto de respuesta
     *
     * @param systemPrompt instrucciones del sistema (rol del asistente)
     * @param userPrompt   mensaje del usuario
     * @return texto generado por el modelo
     */
    public String completar(String systemPrompt, String userPrompt) {
        try {
            // Gemini combina system + user en un único texto de entrada
            String promptCompleto = systemPrompt + "\n\n" + userPrompt;

            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", promptCompleto)
                            ))
                    ),
                    "generationConfig", Map.of(
                            "maxOutputTokens", maxTokens,
                            "temperature", 0.7
                    )
            );

            String bodyJson = objectMapper.writeValueAsString(body);

            // La API key va como query param en Gemini
            String urlConKey = apiUrl + "?key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlConKey))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                log.error("Gemini error - status={} body={}", response.statusCode(), response.body());
                throw new RuntimeException("Error en la API de Gemini: " + response.statusCode());
            }

            // Extraemos el texto de la respuesta de Gemini
            JsonNode json = objectMapper.readTree(response.body());
            return json
                    .path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error llamando a Gemini: {}", e.getMessage());
            throw new RuntimeException("No se pudo conectar con el servicio de IA", e);
        }
    }
}