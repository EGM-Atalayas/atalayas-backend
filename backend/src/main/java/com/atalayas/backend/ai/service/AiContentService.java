package com.atalayas.backend.ai.service;

import com.atalayas.backend.ai.client.GeminiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Generación automática de contenido formativo con Gemini 2.0 Flash
 * El admin describe un tema y la IA genera el contenido estructurado
 * listo para crear un módulo formativo completo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiContentService {

    private final GeminiClient geminiClient;

    private static final String SYSTEM_PROMPT = """
            Eres un experto en formación empresarial y recursos humanos para PYMEs españolas.
            Tu tarea es generar contenido formativo claro, práctico y profesional.
            
            Siempre responde en español.
            Estructura el contenido con secciones bien definidas.
            Usa un lenguaje accesible para empleados sin formación técnica avanzada.
            El contenido debe ser útil, concreto y aplicable al puesto de trabajo.
            No uses markdown excesivo — texto limpio y bien estructurado.
            """;


    /**
     * Genera contenido formativo completo para un módulo
     * Devuelve texto estructurado con introducción, puntos clave y conclusión
     *
     * @param tema        nombre del módulo o tema formativo
     * @param descripcion contexto adicional sobre la empresa o el puesto
     * @param tipoModulo  tipo de módulo (BASICA, ESPECIFICA, IDENTIDAD, etc.)
     * @return contenido formativo generado listo para guardar en ContentItem
     */
    public String generarContenido(String tema, String descripcion, String tipoModulo) {
        log.info("Generando contenido IA - tema={} tipo={}", tema, tipoModulo);

        String userPrompt = String.format("""
                Genera el contenido formativo para el siguiente módulo:
                
                Tema: %s
                Tipo de módulo: %s
                Contexto adicional: %s
                
                El contenido debe incluir:
                1. Introducción (2-3 párrafos explicando la importancia del tema)
                2. Objetivos de aprendizaje (lista de 4-6 puntos claros)
                3. Contenido principal dividido en 3-4 secciones con subtítulos
                4. Puntos clave a recordar (lista de 5-7 puntos breves)
                5. Conclusión (1-2 párrafos)
                
                Extensión: entre 400 y 600 palabras.
                Tono: profesional pero cercano, adaptado a empleados de PYME industrial.
                """, tema, tipoModulo, descripcion != null ? descripcion : "Sin contexto adicional");

        return geminiClient.completar(SYSTEM_PROMPT, userPrompt);
    }


    /**
     * Genera preguntas de evaluación para un contenido formativo
     * Permite crear el quiz de un módulo automáticamente
     *
     * @param contenido   texto del módulo formativo
     * @param numPreguntas número de preguntas (máximo 10)
     * @return preguntas estructuradas con opciones y respuesta correcta
     */
    public String generarPreguntas(String contenido, int numPreguntas) {
        int limite = Math.min(numPreguntas, 10);
        log.info("Generando {} preguntas de evaluación con IA", limite);

        String userPrompt = String.format("""
                Basándote en el siguiente contenido formativo, genera %d preguntas de evaluación tipo test.
                
                CONTENIDO:
                %s
                
                Para cada pregunta usa este formato exacto:
                
                PREGUNTA X: [texto de la pregunta]
                A) [opción]
                B) [opción]
                C) [opción]
                D) [opción]
                CORRECTA: [letra]
                EXPLICACIÓN: [por qué es correcta, en una frase]
                
                Las preguntas deben evaluar comprensión real, no memorización literal.
                """, limite, contenido);

        return geminiClient.completar(SYSTEM_PROMPT, userPrompt);
    }
}