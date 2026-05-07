package com.atalayas.backend.ai.service;

import com.atalayas.backend.ai.client.GroqClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


/**
 * Chatbot de consulta para empleados con llama-3.3-70b-versatile (Groq)
 * Responde preguntas sobre módulos formativos, protocolos internos
 * y dudas sobre el puesto de trabajo en el contexto de su empresa
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final GroqClient groqClient;

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            Eres el asistente virtual de formación de %s, empresa ubicada en
            EGM Atalayas Ciudad Empresarial (Alicante).
            
            Ayudas a los empleados con:
            - Dudas sobre los módulos formativos disponibles
            - Preguntas sobre protocolos y normativas internas
            - Orientación sobre prevención de riesgos laborales (PRL)
            - Información general sobre la empresa y el área empresarial
            - Dudas sobre beneficios y servicios del parque empresarial
            
            Normas:
            - Responde siempre en español, de forma clara y concisa
            - Si no sabes algo con certeza, dilo y sugiere consultar con RRHH
            - Sé amable, profesional y cercano
            - No inventes información específica de la empresa que no se te proporcione
            - Respuestas máximo de 3-4 párrafos, directas y útiles
            """;


    /**
     * Procesa una pregunta del empleado en el contexto de su empresa
     *
     * @param pregunta      mensaje del empleado
     * @param nombreEmpresa nombre de la empresa para personalizar el contexto
     * @param contexto      información adicional (módulos disponibles, etc.)
     * @return respuesta personalizada del asistente
     */
    public String responder(String pregunta, String nombreEmpresa, String contexto) {
        log.info("Chat IA - empresa={} pregunta={}",
                nombreEmpresa,
                pregunta.length() > 50 ? pregunta.substring(0, 50) + "..." : pregunta);

        String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE,
                nombreEmpresa != null ? nombreEmpresa : "EGM Atalayas");

        String userPrompt = (contexto != null && !contexto.isBlank())
                ? String.format("Contexto de la empresa:\n%s\n\nPregunta: %s", contexto, pregunta)
                : pregunta;

        return groqClient.completar(systemPrompt, userPrompt);
    }
}