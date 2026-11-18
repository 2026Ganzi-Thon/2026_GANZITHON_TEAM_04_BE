package com.mallang.domain.thought.exception;

public class CardGenerationException extends RuntimeException {

    public CardGenerationException(String message) {
        super(message);
    }

    public CardGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}