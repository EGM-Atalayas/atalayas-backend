package com.atalayas.backend.ai.client;

import com.atalayas.backend.ai.dto.ChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cliente HTTP hacia la API de OpenAI (gpt-4o-mini).
 * IA principal — todos los servicios de chat, contenido y resúmenes pasan por aquí.
 */
@Slf4j
@Component
public class OpenAiClient {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.model}")
    private String model;

    @Value("${openai.max-tokens}")
    private int maxTokens;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Envía un prompt a OpenAI y devuelve el texto de respuesta.
     *
     * @param systemPrompt instrucciones del sistema
     * @param userPrompt   mensaje del usuario
     * @return texto generado por el modelo
     */
    public String completar(String systemPrompt, String userPrompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "max_tokens", maxTokens,
                    "temperature", 0.7,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user",   "content", userPrompt)
                    )
            );

            String bodyJson = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("OpenAI error - status={} body={}", response.statusCode(), response.body());
                throw new RuntimeException("Error en la API de OpenAI: " + response.statusCode());
            }

            JsonNode json = objectMapper.readTree(response.body());
            return json.path("choices").get(0).path("message").path("content").asText();

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error llamando a OpenAI: {}", e.getMessage());
            throw new RuntimeException("No se pudo conectar con el servicio de IA", e);
        }
    }

    /**
     * Llama a OpenAI con historial de mensajes y escribe el stream de tokens en el OutputStream.
     * Usa la API de streaming SSE: stream: true
     *
     * @param systemPrompt instrucciones del sistema (puede ser null)
     * @param messages     historial de la conversación (role: "user" | "assistant")
     * @param outputStream stream de salida donde se escriben los tokens según llegan
     */
    public void streamCompletions(String systemPrompt, List<ChatMessage> messages, OutputStream outputStream)
            throws IOException {

        List<Map<String, String>> msgs = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            msgs.add(Map.of("role", "system", "content", systemPrompt));
        }
        for (ChatMessage msg : messages) {
            msgs.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "temperature", 0.7,
                "stream", true,
                "messages", msgs
        );

        String bodyJson;
        try {
            bodyJson = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new IOException("Error serializando el body", e);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                .build();

        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                log.error("OpenAI stream error - status={} body_preview={}",
                        response.statusCode(),
                        errorBody.length() > 500 ? errorBody.substring(0, 500) : errorBody);
                if (response.statusCode() == 429) {
                    throw new IOException("El asistente no está disponible en este momento (límite de cuota alcanzado). Inténtalo de nuevo en unos minutos.");
                }
                if (response.statusCode() == 401 || response.statusCode() == 403) {
                    throw new IOException("Error de autenticación con la API de OpenAI. Verifica la configuración del servidor.");
                }
                throw new IOException("Error en la API de OpenAI: status=" + response.statusCode());
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data: ")) continue;
                    String data = line.substring(6).trim();
                    if (data.isEmpty() || "[DONE]".equals(data)) continue;
                    try {
                        JsonNode json = objectMapper.readTree(data);
                        String text = json.path("choices").path(0)
                                .path("delta").path("content").asText("");
                        if (!text.isEmpty()) {
                            outputStream.write(text.getBytes(StandardCharsets.UTF_8));
                            outputStream.flush();
                        }
                    } catch (Exception e) {
                        log.debug("SSE chunk ignorado: {}", e.getMessage());
                    }
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Streaming interrumpido", e);
        }
    }
}

