package com.atalayas.backend.ai.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * Body del endpoint POST /api/v1/ai/chat (streaming).
 */
@Data
public class ChatRequest {

    @NotEmpty(message = "Se requiere al menos un mensaje")
    private List<ChatMessage> messages;

    private String systemPrompt;
}

