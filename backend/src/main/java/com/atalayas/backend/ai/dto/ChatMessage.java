package com.atalayas.backend.ai.dto;

import lombok.Data;

/**
 * Mensaje individual de la conversación del chatbot.
 * role: "user" | "assistant"
 */
@Data
public class ChatMessage {
    private String role;    // "user" o "assistant"
    private String content;
}

