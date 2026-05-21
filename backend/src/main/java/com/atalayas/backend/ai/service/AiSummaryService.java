package com.atalayas.backend.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Generación de resúmenes automáticos con Groq (fallback a Gemini).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiSummaryService {

    private final AiProviderService aiProviderService;

    private static final String SYSTEM_PROMPT = """
            Eres un asistente especializado en simplificar documentos empresariales para PYMEs.
            Tu tarea es generar resúmenes claros, útiles y fieles al contenido original.
            
            Siempre en español.
            Lenguaje claro y directo, sin tecnicismos innecesarios.
            Nunca añadas información que no esté en el texto original.
            """;

    /**
     * Genera un resumen estructurado de un texto largo
     *
     * @param texto texto a resumir (manual, normativa, documento interno)
     * @return resumen con idea principal y puntos clave
     */
    public String resumir(String texto) {
        log.info("Generando resumen IA - {} chars", texto.length());

        String userPrompt = String.format("""
                Resume el siguiente documento de forma clara y estructurada:
                
                %s
                
                Formato de respuesta:
                
                IDEA PRINCIPAL:
                [1-2 frases que capturen la esencia del documento]
                
                PUNTOS CLAVE:
                - [punto 1]
                - [punto 2]
                - [punto 3]
                - [punto 4]
                - [punto 5]
                
                LO QUE EL EMPLEADO DEBE RECORDAR:
                [2-3 frases con lo más importante para el día a día]
                
                Máximo 200 palabras en total.
                """, texto);

        return aiProviderService.completar(SYSTEM_PROMPT, userPrompt);
    }
}