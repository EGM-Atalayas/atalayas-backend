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
            Eres un experto en diseño instruccional y formación corporativa para empresas españolas.
            Tu tarea es crear contenido formativo de alta calidad, práctico y bien estructurado en Markdown.

            Principios que debes seguir siempre:
            - Responde siempre en español.
            - Usa Markdown correctamente: ## para secciones, **negrita** para términos clave, > para advertencias.
            - Lenguaje claro y directo, accesible para empleados sin formación técnica avanzada.
            - Contenido concreto, con ejemplos reales aplicables al puesto de trabajo.
            - Cada sección debe tener sustancia: mínimo 2 párrafos o una lista bien desarrollada.
            - No repitas información entre secciones.
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
                Crea el contenido formativo completo en Markdown para el siguiente módulo:

                Tema: %s
                Tipo de módulo: %s
                Contexto adicional: %s

                Estructura obligatoria:

                ## Introducción
                (2-3 párrafos que expliquen el contexto del tema, su importancia en el entorno laboral y qué va a aprender el empleado)

                ## Objetivos de aprendizaje
                (lista con guión - de 5-6 objetivos específicos y medibles, comenzando con verbos de acción: "Identificar...", "Aplicar...", "Reconocer...")

                ## [Sección 1: primer concepto o bloque temático clave]
                (2-3 párrafos con explicación detallada + al menos un ejemplo práctico del entorno industrial/empresarial)

                ## [Sección 2: segundo concepto o bloque temático clave]
                (igual de detallada)

                ## [Sección 3 y siguientes si el tema lo requiere]
                (mínimo 3 secciones temáticas en total)

                ## Puntos clave a recordar
                (lista con guión - de 6-8 puntos breves, directos y memorables — los más importantes del módulo)

                ## Conclusión
                (1-2 párrafos que refuercen el aprendizaje y motiven al empleado a aplicar lo aprendido)

                Usa **negrita** para términos técnicos importantes la primera vez que aparecen.
                Usa > para notas de seguridad o advertencias críticas.
                Extensión mínima: 800 palabras. Tono profesional pero cercano.
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
                Basándote en el siguiente contenido formativo, genera exactamente %d preguntas de evaluación tipo test.

                CONTENIDO:
                %s

                Reglas para las preguntas:
                - Evalúan comprensión y aplicación real, no memorización literal de frases del texto.
                - Dificultad variada: mezcla preguntas conceptuales, de aplicación práctica y de situaciones reales.
                - Cada pregunta debe tener 4 opciones plausibles — los distractores deben ser razonables, no obviamente incorrectos.
                - La opción correcta no debe ser siempre la misma letra.

                Para cada pregunta usa exactamente este formato:

                PREGUNTA X: [texto claro y sin ambigüedad]
                A) [opción]
                B) [opción]
                C) [opción]
                D) [opción]
                CORRECTA: [letra]
                EXPLICACIÓN: [por qué es correcta y por qué las demás no, en 1-2 frases]

                Genera las %d preguntas completas, sin omitir ninguna.
                """, limite, contenido, limite);

        return geminiClient.completar(SYSTEM_PROMPT, userPrompt);
    }
}