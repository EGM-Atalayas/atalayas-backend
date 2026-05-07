package com.atalayas.backend.ai.client;

public class RateLimitException extends RuntimeException {
    public RateLimitException(String message) {
        super(message);
    }
}


