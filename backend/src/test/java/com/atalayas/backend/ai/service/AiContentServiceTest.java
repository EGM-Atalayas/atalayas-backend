package com.atalayas.backend.ai.service;

import com.atalayas.backend.ai.client.GeminiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiContentServiceTest {

    @Mock GeminiClient geminiClient;
    @InjectMocks AiContentService service;

    // ── generarContenido ─────────────────────────────────────────────────────

    @Test
    void generarContenido_delegaAGeminiYRetornaRespuesta() {
        when(geminiClient.completar(any(), any())).thenReturn("## Contenido generado");

        String result = service.generarContenido("Seguridad laboral", "Empresa industrial", "BASICA");

        assertThat(result).isEqualTo("## Contenido generado");
        verify(geminiClient).completar(any(), contains("Seguridad laboral"));
    }

    @Test
    void generarContenido_sinDescripcion_noLanzaExcepcion() {
        when(geminiClient.completar(any(), any())).thenReturn("contenido");

        assertThat(service.generarContenido("Tema", null, "ESPECIFICA")).isEqualTo("contenido");
    }

    @Test
    void generarContenido_promtIncluyeTipoModulo() {
        when(geminiClient.completar(any(), any())).thenReturn("x");

        service.generarContenido("PRL", "desc", "IDENTIDAD");

        verify(geminiClient).completar(any(), and(contains("PRL"), contains("IDENTIDAD")));
    }

    // ── generarPreguntas ─────────────────────────────────────────────────────

    @Test
    void generarPreguntas_delegaAGemini() {
        when(geminiClient.completar(any(), any())).thenReturn("PREGUNTA 1: ...");

        String result = service.generarPreguntas("contenido del módulo", 5);

        assertThat(result).contains("PREGUNTA 1");
        verify(geminiClient).completar(any(), any());
    }

    @Test
    void generarPreguntas_limiteMax10() {
        when(geminiClient.completar(any(), any())).thenReturn("ok");

        service.generarPreguntas("contenido", 20);

        verify(geminiClient).completar(any(), contains("10")); // límite aplicado
    }

    @Test
    void generarPreguntas_numPreguntasDentroRango_noCortado() {
        when(geminiClient.completar(any(), any())).thenReturn("ok");

        service.generarPreguntas("contenido", 3);

        verify(geminiClient).completar(any(), contains("3"));
    }
}

