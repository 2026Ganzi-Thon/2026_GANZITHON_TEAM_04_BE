package com.mallang.common.exception;

import java.util.Objects;

import org.springframework.http.HttpStatus;

public class GeneralException extends RuntimeException {

    private final HttpStatus status;

    public GeneralException(HttpStatus status, String message) {
        super(message);
        this.status = Objects.requireNonNull(status, "HTTP status is required");
    }

    public HttpStatus getStatus() {
        return status;
    }
}
