package com.atalayas.backend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Respuesta del endpoint POST /api/v1/ai/generar-desde-archivo.
 * Contiene el contenido formativo generado a partir de un archivo subido.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiFileResponse {
    private String titulo;
    private String descripcion;
    /** Contenido formativo en Markdown (cuando tiposSalida incluye "documentacion") */
    private String contenido;
    /** Guion conversacional de podcast (cuando tiposSalida incluye "podcast") */
    private String scriptPodcast;
    /** JSON de slides para video (cuando tiposSalida incluye "video") */
    private String scriptVideo;
    /** URL pública del MP3 del podcast (Supabase Storage) */
    private String podcastAudioUrl;
    /** Tipos de contenido generados, separados por coma */
    private String tiposSalida;
    private String modelo;
    private OffsetDateTime generadoEn;
}
