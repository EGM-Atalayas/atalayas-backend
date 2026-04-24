package com.atalayas.backend.ai.controller;

import com.atalayas.backend.ai.client.ElevenLabsClient;
import com.atalayas.backend.ai.client.GeminiClient;
import com.atalayas.backend.ai.client.GroqClient;
import com.atalayas.backend.ai.dto.AiFileResponse;
import com.atalayas.backend.ai.dto.AiPromptRequest;
import com.atalayas.backend.ai.dto.AiResponse;
import com.atalayas.backend.ai.dto.ChatRequest;
import com.atalayas.backend.ai.service.AiContentService;
import com.atalayas.backend.ai.service.AiFileService;
import com.atalayas.backend.ai.service.AiSummaryService;
import com.atalayas.backend.ai.service.SupabaseStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Endpoints de inteligencia artificial con Gemini 2.0 Flash
 *
 * Cubre cuatro casos de uso:
 *   - Generación de contenido formativo completo para un módulo
 *   - Generación de preguntas de evaluación para un contenido
 *   - Chatbot de consulta para empleados con contexto de empresa
 *   - Resumen estructurado de textos largos o documentos
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "Inteligencia Artificial",
        description = "Generación de contenido y chatbot con Gemini 2.0 Flash")
@SecurityRequirement(name = "bearerAuth")
public class AiController {

    private final AiContentService aiContentService;
    private final AiSummaryService aiSummaryService;
    private final AiFileService aiFileService;
    private final GeminiClient geminiClient;
    private final GroqClient groqClient;
    private final ElevenLabsClient elevenLabsClient;
    private final SupabaseStorageService supabaseStorageService;


    /**
     * POST /api/v1/ai/generar-contenido
     * Genera contenido formativo completo para un módulo.
     * Solo ROLE_ADMIN y ROLE_ADMIN_EMPRESA.
     * 400 si tema o tipoModulo están vacíos  500 si Gemini falla.
     */
    @PostMapping("/generar-contenido")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Generar contenido formativo con IA",
            description = "Genera contenido formativo completo para un módulo usando Gemini 2.0 Flash. Requiere `tema` y `tipoModulo`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contenido generado correctamente"),
            @ApiResponse(responseCode = "400", description = "Tema o tipoModulo vacíos",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
            @ApiResponse(responseCode = "500", description = "Error en la API de Gemini",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<AiResponse> generarContenido(
            @Valid @RequestBody AiPromptRequest request) {

        String contenido = aiContentService.generarContenido(
                request.getTema(),
                request.getDescripcion(),
                request.getTipoModulo()
        );

        return ResponseEntity.ok(AiResponse.builder()
                .contenido(contenido)
                .modelo("gemini-2.0-flash")
                .generadoEn(OffsetDateTime.now())
                .build());
    }


    /**
     * POST /api/v1/ai/generar-preguntas
     * Genera preguntas de evaluación para un contenido dado.
     * Solo ROLE_ADMIN y ROLE_ADMIN_EMPRESA.
     * numPreguntas es opcional — por defecto 5.
     */
    @PostMapping("/generar-preguntas")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Generar preguntas de evaluación con IA",
            description = "Genera preguntas de tipo test para un contenido dado. `numPreguntas` es opcional (por defecto 5).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preguntas generadas correctamente"),
            @ApiResponse(responseCode = "400", description = "Prompt vacío",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
            @ApiResponse(responseCode = "500", description = "Error en la API de Gemini",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<AiResponse> generarPreguntas(
            @Valid @RequestBody AiPromptRequest request) {

        int num = request.getNumPreguntas() != null ? request.getNumPreguntas() : 5;
        String preguntas = aiContentService.generarPreguntas(request.getPrompt(), num);

        return ResponseEntity.ok(AiResponse.builder()
                .contenido(preguntas)
                .modelo("gemini-2.0-flash")
                .generadoEn(OffsetDateTime.now())
                .build());
    }


    /**
     * POST /api/v1/ai/chat
     * Chatbot con historial de conversación — responde en streaming (text/plain).
     * Body: { messages: [{role, content}], systemPrompt: string }
     * Todos los usuarios autenticados pueden usarlo.
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Chatbot con streaming",
            description = "Recibe el historial de mensajes y un systemPrompt, devuelve la respuesta de Gemini como stream de texto plano.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stream de tokens del chatbot"),
            @ApiResponse(responseCode = "400", description = "Messages vacío",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
            @ApiResponse(responseCode = "500", description = "Error en la API de Gemini",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<StreamingResponseBody> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Chat streaming - {} mensajes", request.getMessages().size());
        StreamingResponseBody stream = outputStream ->
                geminiClient.streamCompletions(request.getSystemPrompt(), request.getMessages(), outputStream);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(stream);
    }


    /**
     * POST /api/v1/ai/resumir
     * Genera un resumen estructurado de un texto largo o documento.
     * Solo ROLE_ADMIN y ROLE_ADMIN_EMPRESA.
     */
    @PostMapping("/resumir")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Resumir texto o documento con IA",
            description = "Genera un resumen estructurado de un texto largo. El texto se envía en el campo `prompt`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen generado correctamente"),
            @ApiResponse(responseCode = "400", description = "Texto vacío",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
            @ApiResponse(responseCode = "500", description = "Error en la API de Gemini",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<AiResponse> resumir(
            @Valid @RequestBody AiPromptRequest request) {

        String resumen = aiSummaryService.resumir(request.getPrompt());

        return ResponseEntity.ok(AiResponse.builder()
                .contenido(resumen)
                .modelo("gemini-2.0-flash")
                .generadoEn(OffsetDateTime.now())
                .build());
    }


    /**
     * POST /api/v1/ai/generar-desde-archivo
     * Acepta un archivo (PDF, DOCX o TXT), extrae su texto y lo usa como base
     * para que Gemini genere un contenido formativo estructurado.
     * Solo ROLE_ADMIN y ROLE_ADMIN_EMPRESA.
     * Devuelve { titulo, descripcion, contenido, modelo, generadoEn }.
     */
    /**
     * POST /api/v1/ai/generar-desde-archivo
     *
     * Acepta un archivo (PDF, DOCX o TXT) y uno o varios tipos de salida:
     *   - documentacion : contenido formativo en Markdown
     *   - podcast       : guion conversacional apto para narración TTS
     *   - video         : JSON de slides para presentación
     *
     * Se puede combinar: "documentacion,podcast", "documentacion,video", etc.
     * Por defecto genera solo documentación.
     */
    @PostMapping(value = "/generar-desde-archivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    @Operation(summary = "Generar contenido formativo a partir de un archivo",
            description = "Sube un archivo PDF, DOCX o TXT y especifica los tipos de salida deseados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contenido generado correctamente"),
            @ApiResponse(responseCode = "400", description = "Archivo vacío o formato no soportado"),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente"),
            @ApiResponse(responseCode = "500", description = "Error en la API de Gemini o al leer el archivo")
    })
    public ResponseEntity<AiFileResponse> generarDesdeArchivo(
            @RequestPart("archivo") MultipartFile archivo,
            @RequestParam(value = "tiposSalida", defaultValue = "documentacion") String tiposSalida) {

        // 1. Extraer texto del archivo
        String textoExtraido;
        try {
            textoExtraido = aiFileService.extraerTexto(archivo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            throw new RuntimeException("Error al leer el archivo: " + e.getMessage(), e);
        }

        if (textoExtraido.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // Limitar texto a 25 000 caracteres (llama-3.3-70b soporta 128k tokens)
        String textoParaPrompt = textoExtraido.length() > 25_000
                ? textoExtraido.substring(0, 25_000)
                : textoExtraido;

        boolean incluirDoc     = tiposSalida.contains("documentacion");
        boolean incluirPodcast = tiposSalida.contains("podcast");
        boolean incluirVideo   = tiposSalida.contains("video");

        // 2. Construir prompt dinámico según tipos solicitados
        StringBuilder camposJson = new StringBuilder();
        camposJson.append("- \"titulo\": título descriptivo del módulo formativo, máximo 80 caracteres, que refleje el tema del documento.\n");
        camposJson.append("- \"descripcion\": resumen atractivo de 2-3 frases (máximo 300 caracteres) que explique qué aprenderá el empleado.\n");
        if (incluirDoc) {
            camposJson.append("""
                - "contenido": documentación formativa completa en Markdown. Sigue esta estructura exacta:
                  ## Introducción
                  (2-3 párrafos explicando el contexto y la importancia del tema para el empleado)
                  ## Objetivos de aprendizaje
                  (lista con guión - de 4-6 objetivos claros y medibles, comenzando con verbos de acción)
                  ## [Título de la primera sección temática]
                  (2-4 párrafos con explicación detallada, ejemplos prácticos y aplicaciones reales del documento fuente)
                  ## [Título de la segunda sección temática]
                  (igual de detallada, extrae y amplía el contenido del documento)
                  ## [Más secciones temáticas si el documento lo requiere, mínimo 3 en total]
                  ## Puntos clave a recordar
                  (lista con guión - de 6-8 puntos breves y memorables, los más importantes del documento)
                  ## Conclusión
                  (1-2 párrafos que refuercen el aprendizaje y motiven a aplicarlo)
                  Usa **negrita** para términos técnicos importantes. Usa > para advertencias o notas críticas de seguridad.
                  Extensión mínima: 900 palabras. Sé fiel al contenido del documento fuente, no inventes información.
                """);
        }
        if (incluirPodcast) {
            camposJson.append("""
                - "scriptPodcast": guion completo para un episodio de podcast formativo de 6-8 minutos (aproximadamente 900-1100 palabras).
                  Formato: texto continuo en español, tono conversacional y cercano, como si un formador experto explicara el tema en voz alta.
                  Estructura obligatoria:
                    1. Bienvenida y presentación del tema (30 segundos)
                    2. Por qué es importante este tema para el empleado (1 minuto)
                    3. Desarrollo del contenido en 3-4 bloques temáticos con transiciones naturales (4-5 minutos)
                    4. Resumen de los 3 puntos más importantes (1 minuto)
                    5. Cierre motivador y llamada a la acción (30 segundos)
                  Usa transiciones como "Pasemos ahora a...", "Como acabamos de ver...", "Esto es importante porque...".
                  NO uses markdown, asteriscos, guiones ni ningún símbolo especial. Solo texto plano listo para síntesis de voz (TTS).
                  NO uses abreviaturas. Escribe los números con letras. Evita siglas sin explicarlas primero.
                """);
        }
        if (incluirVideo) {
            camposJson.append("""
                - "scriptVideo": array JSON de entre 8 y 12 slides para una presentación formativa. Cada slide tiene exactamente estos campos:
                  { "numero": N, "titulo": "Título conciso de la slide (máximo 50 caracteres)", "contenido": ["punto clave 1", "punto clave 2", "punto clave 3"], "notas": "Texto de apoyo del presentador para esta slide, explicando los puntos con más detalle (2-4 frases completas)" }
                  Distribución obligatoria:
                    - Slide 1: portada (título del módulo + subtítulo descriptivo, máximo 2 puntos)
                    - Slides 2-N: contenido temático (máximo 4 puntos por slide, concisos y accionables)
                    - Slide final: resumen con los 3-5 puntos más importantes + llamada a la acción
                  Las notas del presentador deben ser lo suficientemente detalladas para hablar durante 30-60 segundos por slide.
                """);
        }

        String systemPrompt = """
                Eres un experto en diseño instruccional y formación corporativa para empresas españolas.
                Tu objetivo es transformar el documento que te proporciona el usuario en material formativo de alta calidad para empleados.

                REGLA CRÍTICA DE FORMATO:
                - Responde ÚNICAMENTE con un objeto JSON válido y nada más.
                - NO uses bloques de código Markdown (sin ```json ni ```).
                - NO añadas comentarios, explicaciones ni texto fuera del JSON.
                - Los valores de texto dentro del JSON (como "contenido" o "scriptPodcast") son cadenas de texto plano o Markdown.
                - NUNCA incluyas JSON, llaves { } ni comillas de JSON dentro del valor de "contenido". Solo Markdown puro.
                - Si el valor de un campo de texto necesita saltos de línea, usa \\n dentro de la cadena JSON.

                El contenido debe ser fiel al documento fuente: extrae, organiza y amplía su información, no inventes datos.
                Idioma: español. Tono: profesional pero cercano, accesible para empleados sin formación técnica avanzada.
                Claves requeridas del JSON:
                """ + camposJson;

        String userPrompt = "Transforma el siguiente documento en contenido formativo de calidad:\n\n" + textoParaPrompt;

        // 3. Llamar a Groq (texto: documentación + video)
        String respuestaRaw = groqClient.completar(systemPrompt, userPrompt);

        // 4. Parsear campos del JSON devuelto
        String titulo        = extraerCampoJson(respuestaRaw, "titulo");
        String descripcion   = extraerCampoJson(respuestaRaw, "descripcion");
        String contenido     = incluirDoc     ? extraerCampoJson(respuestaRaw, "contenido")      : null;
        String scriptPodcast = incluirPodcast ? extraerCampoJson(respuestaRaw, "scriptPodcast")  : null;
        String scriptVideo   = incluirVideo   ? extraerCampoJsonRaw(respuestaRaw, "scriptVideo") : null;

        // Fallback si el parseo falla por completo
        if (titulo.isBlank() && descripcion.isBlank()) {
            contenido = respuestaRaw;
            titulo = "";
        }

        // Sanear el contenido: si el modelo coló JSON o bloques de código al principio, eliminarlos
        if (contenido != null) {
            contenido = sanitizarContenidoMarkdown(contenido);
        }

        // 5. Generar audio MP3 con ElevenLabs si se pidió podcast
        String podcastAudioUrl = null;
        if (incluirPodcast && scriptPodcast != null && !scriptPodcast.isBlank()) {
            try {
                byte[] audioBytes = elevenLabsClient.textToSpeech(scriptPodcast);
                // Usamos un UUID temporal para el nombre del archivo; el frontend lo asociará al módulo después
                podcastAudioUrl = supabaseStorageService.subirAudioPodcast(
                        audioBytes,
                        UUID.randomUUID()
                );
            } catch (Exception e) {
                log.warn("No se pudo generar el audio del podcast: {}. Se continuará sin audio.", e.getMessage());
                // No bloqueamos la respuesta si el audio falla
            }
        }

        return ResponseEntity.ok(AiFileResponse.builder()
                .titulo(titulo)
                .descripcion(descripcion)
                .contenido(contenido)
                .scriptPodcast(scriptPodcast)
                .scriptVideo(scriptVideo)
                .podcastAudioUrl(podcastAudioUrl)
                .tiposSalida(tiposSalida)
                .modelo("llama-3.3-70b-versatile")
                .generadoEn(OffsetDateTime.now())
                .build());
    }

    /**
     * Extrae el valor de un campo de un JSON simple sin librería,
     * usando la ObjectMapper ya disponible en el contexto de Spring (Jackson).
     * Si el JSON no es válido devuelve cadena vacía.
     */
    /** Extrae el valor de texto de un campo JSON. */
    private String extraerCampoJson(String json, String campo) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            String limpio = limpiarJson(json);
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(limpio);
            return node.path(campo).asText("");
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Extrae un campo JSON que puede ser un objeto o array, devolviéndolo serializado como String.
     * Útil para scriptVideo que es un array de slides.
     */
    private String extraerCampoJsonRaw(String json, String campo) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            String limpio = limpiarJson(json);
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(limpio);
            com.fasterxml.jackson.databind.JsonNode campo_node = node.path(campo);
            if (campo_node.isMissingNode()) return null;
            return mapper.writeValueAsString(campo_node);
        } catch (Exception e) {
            return null;
        }
    }

    private String limpiarJson(String json) {
        String limpio = json.trim();
        if (limpio.startsWith("```")) {
            limpio = limpio.replaceFirst("```(?:json)?", "").replaceAll("```\\s*$", "").trim();
        }
        return limpio;
    }

    /**
     * Limpia el campo "contenido" cuando el modelo de IA cuela la cabecera JSON
     * dentro del propio valor del campo (p.ej. empieza con ```json\n{\n"titulo"...).
     *
     * Estrategia:
     * 1. Si empieza con un bloque de código (```), quita todo hasta el primer ## o hasta
     *    la primera línea que no sea JSON.
     * 2. Si empieza con { (JSON inline), intenta extraer el campo "contenido" del JSON anidado.
     * 3. Si el contenido tiene un bloque JSON al principio seguido de Markdown, recorta el bloque JSON.
     */
    private String sanitizarContenidoMarkdown(String contenido) {
        if (contenido == null || contenido.isBlank()) return contenido;

        String limpio = contenido.trim();

        // Caso 1: empieza con ```json ... → el modelo metió JSON en la cadena
        if (limpio.startsWith("```")) {
            // Si hay un ## después del bloque de código, quedarnos solo con lo que viene después
            int idxPrimerHash = limpio.indexOf("\n##");
            if (idxPrimerHash == -1) idxPrimerHash = limpio.indexOf("\n# ");
            if (idxPrimerHash > 0) {
                return limpio.substring(idxPrimerHash).trim();
            }
            // Si no hay headers Markdown, intentar extraer campo "contenido" del JSON embebido
            try {
                String jsonEmbebido = limpio.replaceFirst("```(?:json)?\\s*", "").replaceAll("```\\s*$", "").trim();
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(jsonEmbebido);
                String contenidoAnidado = node.path("contenido").asText("");
                if (!contenidoAnidado.isBlank()) return contenidoAnidado.trim();
            } catch (Exception ignored) {}
            // Último recurso: quitar el bloque de código y devolver lo que quede
            return limpio.replaceFirst("```(?:json)?[\\s\\S]*?```", "").trim();
        }

        // Caso 2: empieza con { → podría ser JSON puro como valor del contenido
        if (limpio.startsWith("{")) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(limpio);
                String contenidoAnidado = node.path("contenido").asText("");
                if (!contenidoAnidado.isBlank()) return contenidoAnidado.trim();
            } catch (Exception ignored) {}
            // Si no es JSON válido, devolver tal cual (podría ser Markdown que empieza con {)
        }

        // Caso 3: tiene un bloque JSON o código al principio seguido de Markdown
        // Buscar el primer encabezado Markdown (## o #) y recortar desde ahí
        int idxMarkdown = limpio.indexOf("\n##");
        if (idxMarkdown == -1) idxMarkdown = limpio.indexOf("\n# ");
        if (idxMarkdown > 50) { // Solo recortar si hay bastante contenido basura antes
            String antes = limpio.substring(0, idxMarkdown);
            // Verificar que "antes" parece JSON (contiene "titulo" o "descripcion")
            if (antes.contains("\"titulo\"") || antes.contains("\"descripcion\"") || antes.contains("```")) {
                return limpio.substring(idxMarkdown).trim();
            }
        }

        return limpio;
    }
}