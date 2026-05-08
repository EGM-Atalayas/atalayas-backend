package com.atalayas.backend.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

/**
 * Sube archivos al Storage de Supabase desde el backend.
 * Se usa para los MP3 de podcast generados por ElevenLabs.
 */
@Slf4j
@Service
public class SupabaseStorageService {

    @Value("${supabase.url:}")
    private String supabaseUrl;

    @Value("${supabase.service-key:}")
    private String serviceKey;

    private final HttpClient httpClient;

    public SupabaseStorageService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Sube cualquier archivo al bucket "modulos" de Supabase Storage.
     *
     * @param bytes       contenido del archivo
     * @param contentType MIME type (image/jpeg, application/pdf, etc.)
     * @param carpeta     subcarpeta dentro del bucket (portadas, adjuntos, etc.)
     * @param fileName    nombre final del archivo (con extensión)
     * @return URL pública del archivo subido
     */
    public String subirArchivo(byte[] bytes, String contentType, String carpeta, String fileName) {
        if (supabaseUrl.isBlank() || serviceKey.isBlank()) {
            throw new RuntimeException("Supabase no está configurado (SUPABASE_URL / SUPABASE_SERVICE_KEY)");
        }

        String ruta       = carpeta + "/" + fileName;
        String uploadUrl  = supabaseUrl + "/storage/v1/object/modulos/" + ruta;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uploadUrl))
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + serviceKey)
                    .header("Content-Type", contentType)
                    .header("x-upsert", "true")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200 && response.statusCode() != 201) {
                log.error("Supabase Storage error - status={} body={}", response.statusCode(), response.body());
                throw new RuntimeException("Error subiendo archivo a Supabase: " + response.statusCode());
            }

            return supabaseUrl + "/storage/v1/object/public/modulos/" + ruta;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error subiendo archivo a Supabase: {}", e.getMessage());
            throw new RuntimeException("No se pudo subir el archivo", e);
        }
    }

    /**
     * Sube un archivo MP3 al bucket "modulos" de Supabase Storage.
     *
     * @param audioBytes bytes del archivo MP3
     * @param moduloId   UUID del módulo (para nombrar el archivo)
     * @return URL pública del archivo subido
     */
    public String subirAudioPodcast(byte[] audioBytes, UUID moduloId) {
        if (supabaseUrl.isBlank() || serviceKey.isBlank()) {
            throw new RuntimeException("Supabase no está configurado (SUPABASE_URL / SUPABASE_SERVICE_KEY)");
        }

        String fileName = "podcast/" + moduloId + ".mp3";
        String uploadUrl = supabaseUrl + "/storage/v1/object/modulos/" + fileName;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uploadUrl))
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + serviceKey)
                    .header("Content-Type", "audio/mpeg")
                    .header("x-upsert", "true")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(audioBytes))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200 && response.statusCode() != 201) {
                log.error("Supabase Storage error - status={} body={}", response.statusCode(), response.body());
                throw new RuntimeException("Error subiendo audio a Supabase: " + response.statusCode());
            }

            // URL pública del archivo
            return supabaseUrl + "/storage/v1/object/public/modulos/" + fileName;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error subiendo audio a Supabase: {}", e.getMessage());
            throw new RuntimeException("No se pudo subir el audio del podcast", e);
        }
    }
}
