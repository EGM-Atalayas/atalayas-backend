package com.atalayas.backend.ai.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Cliente HTTP hacia la API de ElevenLabs Text-to-Speech.
 * Convierte el guion de podcast en audio MP3.
 */
@Slf4j
@Component
public class ElevenLabsClient {

    @Value("${elevenlabs.api.key:}")
    private String apiKey;

    @Value("${elevenlabs.voice.id:JBFqnCBsd6RMkjVDRZzb}")
    private String voiceId;

    @Value("${elevenlabs.model.id:eleven_multilingual_v2}")
    private String modelId;

    private static final String BASE_URL = "https://api.elevenlabs.io/v1/text-to-speech/";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ElevenLabsClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Convierte texto en audio MP3 usando ElevenLabs TTS.
     *
     * @param texto el guion del podcast
     * @return bytes del archivo MP3 generado
     */
    public byte[] textToSpeech(String texto) {
        try {
            // Limitar a 5000 caracteres (ElevenLabs free tier limit)
            String textoLimitado = texto.length() > 5000 ? texto.substring(0, 5000) : texto;

            Map<String, Object> body = Map.of(
                    "text", textoLimitado,
                    "model_id", modelId,
                    "voice_settings", Map.of(
                            "stability", 0.5,
                            "similarity_boost", 0.75,
                            "style", 0.0,
                            "use_speaker_boost", true
                    )
            );

            String bodyJson = objectMapper.writeValueAsString(body);
            String url = BASE_URL + voiceId;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .header("Accept", "audio/mpeg")
                    .header("xi-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            HttpResponse<byte[]> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofByteArray()
            );

            if (response.statusCode() != 200) {
                log.error("ElevenLabs error - status={}", response.statusCode());
                throw new RuntimeException("Error en la API de ElevenLabs: " + response.statusCode());
            }

            return response.body();

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error llamando a ElevenLabs: {}", e.getMessage());
            throw new RuntimeException("No se pudo generar el audio del podcast", e);
        }
    }
}
