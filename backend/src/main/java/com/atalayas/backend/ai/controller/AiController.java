package com.atalayas.backend.ai.controller;

import com.atalayas.backend.ai.dto.AiPromptRequest;
import com.atalayas.backend.ai.dto.AiResponse;
import com.atalayas.backend.ai.service.AiChatService;
import com.atalayas.backend.ai.service.AiContentService;
import com.atalayas.backend.ai.service.AiSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    @Operation(summary = "Generar contenido formativo con IA")
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
    @Operation(summary = "Generar preguntas de evaluación con IA")
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
    @Operation(summary = "Chatbot de consulta para empleados")
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
    @Operation(summary = "Resumir texto o documento con IA")
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