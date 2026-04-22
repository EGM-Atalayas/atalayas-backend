package com.atalayas.backend.ai.controller;

import com.atalayas.backend.ai.client.GeminiClient;
import com.atalayas.backend.ai.dto.AiFileResponse;
import com.atalayas.backend.ai.dto.AiPromptRequest;
import com.atalayas.backend.ai.dto.AiResponse;
import com.atalayas.backend.ai.service.AiChatService;
import com.atalayas.backend.ai.service.AiContentService;
import com.atalayas.backend.ai.service.AiFileService;
import com.atalayas.backend.ai.service.AiSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * Endpoints de inteligencia artificial con Gemini 2.0 Flash
 *
 * Cubre cuatro casos de uso:
 *   - Generación de contenido formativo completo para un módulo
 *   - Generación de preguntas de evaluación para un contenido
 *   - Chatbot de consulta para empleados con contexto de empresa
 *   - Resumen estructurado de textos largos o documentos
 */
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "Inteligencia Artificial",
        description = "Generación de contenido y chatbot con Gemini 2.0 Flash")
@SecurityRequirement(name = "bearerAuth")
public class AiController {

    private final AiContentService aiContentService;
    private final AiChatService aiChatService;
    private final AiSummaryService aiSummaryService;
    private final AiFileService aiFileService;
    private final GeminiClient geminiClient;


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
     * Chatbot de consulta para empleados con contexto de empresa.
     * Todos los usuarios autenticados pueden usarlo.
     */
    @PostMapping("/chat")
    @Operation(summary = "Chatbot de consulta para empleados",
            description = "Chatbot IA accesible a todos los usuarios autenticados. Acepta `prompt`, `nombreEmpresa` y `contexto` adicional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Respuesta del chatbot"),
            @ApiResponse(responseCode = "400", description = "Prompt vacío",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
            @ApiResponse(responseCode = "500", description = "Error en la API de Gemini",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<AiResponse> chat(
            @Valid @RequestBody AiPromptRequest request) {

        String respuesta = aiChatService.responder(
                request.getPrompt(),
                request.getNombreEmpresa(),
                request.getContexto()
        );

        return ResponseEntity.ok(AiResponse.builder()
                .contenido(respuesta)
                .modelo("gemini-2.0-flash")
                .generadoEn(OffsetDateTime.now())
                .build());
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

        // Limitar texto a 12 000 caracteres para no exceder el contexto de Gemini
        String textoParaPrompt = textoExtraido.length() > 12_000
                ? textoExtraido.substring(0, 12_000)
                : textoExtraido;

        boolean incluirDoc     = tiposSalida.contains("documentacion");
        boolean incluirPodcast = tiposSalida.contains("podcast");
        boolean incluirVideo   = tiposSalida.contains("video");

        // 2. Construir prompt dinámico según tipos solicitados
        StringBuilder camposJson = new StringBuilder();
        camposJson.append("- \"titulo\": título breve del módulo (máximo 80 caracteres).\n");
        camposJson.append("- \"descripcion\": resumen en 2-3 frases (máximo 300 caracteres).\n");
        if (incluirDoc) {
            camposJson.append("- \"contenido\": contenido formativo completo en Markdown con secciones, ejemplos y puntos clave.\n");
        }
        if (incluirPodcast) {
            camposJson.append("- \"scriptPodcast\": guion conversacional de un podcast de ~5 minutos donde un presentador explica el tema de forma amena y clara. Sin marcas de tiempo, solo texto continuo listo para narrar.\n");
        }
        if (incluirVideo) {
            camposJson.append("- \"scriptVideo\": array JSON de slides, cada slide con campos: { \"numero\": número, \"titulo\": título de la slide, \"contenido\": puntos clave en bullet points, \"notas\": notas del presentador }. Genera entre 6 y 10 slides.\n");
        }

        String systemPrompt = """
                Eres un experto en diseño instruccional y formación corporativa.
                A partir del texto que te proporciona el usuario, genera el siguiente contenido en un único JSON válido.
                Responde ÚNICAMENTE con el JSON, sin texto adicional ni bloques de código Markdown.
                Claves requeridas:
                """ + camposJson;

        String userPrompt = "Texto del documento:\n\n" + textoParaPrompt;

        // 3. Llamar a Gemini
        String respuestaRaw = geminiClient.completar(systemPrompt, userPrompt);

        // 4. Parsear campos del JSON devuelto
        String titulo      = extraerCampoJson(respuestaRaw, "titulo");
        String descripcion = extraerCampoJson(respuestaRaw, "descripcion");
        String contenido   = incluirDoc     ? extraerCampoJson(respuestaRaw, "contenido")     : null;
        String scriptPodcast = incluirPodcast ? extraerCampoJson(respuestaRaw, "scriptPodcast") : null;
        String scriptVideo   = incluirVideo   ? extraerCampoJsonRaw(respuestaRaw, "scriptVideo"): null;

        // Fallback si el parseo falla por completo
        if (titulo.isBlank() && descripcion.isBlank()) {
            contenido = respuestaRaw;
            titulo = "";
        }

        return ResponseEntity.ok(AiFileResponse.builder()
                .titulo(titulo)
                .descripcion(descripcion)
                .contenido(contenido)
                .scriptPodcast(scriptPodcast)
                .scriptVideo(scriptVideo)
                .tiposSalida(tiposSalida)
                .modelo("gemini-2.0-flash")
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
}