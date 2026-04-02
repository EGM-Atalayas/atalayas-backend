package com.atalayas.backend.ai.controller;

import com.atalayas.backend.ai.dto.AiPromptRequest;
import com.atalayas.backend.ai.dto.AiResponse;
import com.atalayas.backend.ai.service.AiChatService;
import com.atalayas.backend.ai.service.AiContentService;
import com.atalayas.backend.ai.service.AiSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "Inteligencia Artificial", description = "Generación de contenido y chatbot con Gemini 2.0 Flash")
@SecurityRequirement(name = "bearerAuth")
public class AiController {

    private final AiContentService aiContentService;
    private final AiChatService aiChatService;
    private final AiSummaryService aiSummaryService;

    /**
     * POST /api/v1/ai/generar-contenido
     * Genera contenido formativo completo para un módulo.
     * Solo ROLE_ADMIN y ROLE_ADMIN_EMPRESA.
     * → 400 si tema o tipoModulo están vacíos
     * → 500 si la API de Gemini falla
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
                .generadoEn(LocalDateTime.now())
                .build());
    }


    /**
     * POST /api/v1/ai/generar-preguntas
     * Genera preguntas de evaluación para un contenido dado
     * Solo ROLE_ADMIN y ROLE_ADMIN_EMPRESA
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
                .generadoEn(LocalDateTime.now())
                .build());
    }


    /**
     * POST /api/v1/ai/chat
     * Chatbot de consulta para empleados
     * Todos los usuarios autenticados pueden usarlo
     */
    @PostMapping("/chat")
    @Operation(summary = "Chatbot de consulta para empleados",
               description = "Chatbot IA accesible a todos los usuarios autenticados. Acepta `prompt`, `nombreEmpresa` (contexto) y `contexto` adicional.")
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
                .generadoEn(LocalDateTime.now())
                .build());
    }


    /**
     * POST /api/v1/ai/resumir
     * Genera un resumen estructurado de un texto largo
     * Solo ROLE_ADMIN y ROLE_ADMIN_EMPRESA
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
                .generadoEn(LocalDateTime.now())
                .build());
    }
}