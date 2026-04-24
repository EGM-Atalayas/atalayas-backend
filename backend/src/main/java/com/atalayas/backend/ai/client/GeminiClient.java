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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    /**
     * Llama a Gemini con historial de mensajes y escribe el stream de tokens en el OutputStream.
     * Usa la API de streaming SSE: streamGenerateContent?alt=sse
     *
     * @param systemPrompt  instrucciones del sistema (puede ser null)
     * @param messages      historial de la conversación (role: "user" | "assistant")
     * @param outputStream  stream de salida donde se escriben los tokens según llegan
     */
    public void streamCompletions(String systemPrompt, List<ChatMessage> messages, OutputStream outputStream)
            throws IOException {

        // Convertir mensajes: "assistant" → "model" (formato Gemini)
        List<Map<String, Object>> contents = messages.stream()
                .map(msg -> {
                    String geminiRole = "assistant".equals(msg.getRole()) ? "model" : msg.getRole();
                    return (Map<String, Object>) Map.of(
                            "role", geminiRole,
                            "parts", List.of(Map.of("text", msg.getContent()))
                    );
                })
                .collect(Collectors.toList());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", contents);
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            body.put("systemInstruction", Map.of(
                    "parts", List.of(Map.of("text", systemPrompt))
            ));
        }
        body.put("generationConfig", Map.of(
                "maxOutputTokens", maxTokens,
                "temperature", 0.7
        ));

        String bodyJson = objectMapper.writeValueAsString(body);

        // De generateContent → streamGenerateContent con ?alt=sse para recibir SSE
        String streamUrl = apiUrl.replace("generateContent", "streamGenerateContent")
                + "?alt=sse&key=" + apiKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(streamUrl))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                .build();

        try {
            HttpResponse<InputStream> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofInputStream()
            );

            if (response.statusCode() != 200) {
                String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                log.error("Gemini stream error - status={} body={}", response.statusCode(), errorBody);
                throw new IOException("Error en la API de Gemini: " + response.statusCode());
            }

            // Leer SSE línea a línea y extraer el texto de cada chunk
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data: ")) continue;
                    String data = line.substring(6).trim();
                    if (data.isEmpty()) continue;
                    try {
                        JsonNode json = objectMapper.readTree(data);
                        String text = json
                                .path("candidates").path(0)
                                .path("content").path("parts").path(0)
                                .path("text").asText("");
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