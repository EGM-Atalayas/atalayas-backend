package com.atalayas.backend.ai.service;

import com.atalayas.backend.ai.client.GeminiClient;
import com.atalayas.backend.ai.client.GroqClient;
import com.atalayas.backend.ai.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Orquestador de proveedores IA.
 * Intenta Groq (principal) y hace fallback a Gemini ante cualquier fallo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiProviderService {

    private final GroqClient groqClient;
    private final GeminiClient geminiClient;

    public String completar(String systemPrompt, String userPrompt) {
        try {
            return groqClient.completar(systemPrompt, userPrompt);
        } catch (Exception e) {
            log.warn("Groq falló ({}), usando Gemini como fallback", e.getMessage());
            return geminiClient.completar(systemPrompt, userPrompt);
        }
    }

    public void streamCompletions(String systemPrompt, List<ChatMessage> messages, OutputStream outputStream)
            throws IOException {
        try {
            groqClient.streamCompletions(systemPrompt, messages, outputStream);
        } catch (Exception e) {
            log.warn("Groq stream falló ({}), usando Gemini como fallback", e.getMessage());
            geminiClient.streamCompletions(systemPrompt, messages, outputStream);
        }
    }
}

