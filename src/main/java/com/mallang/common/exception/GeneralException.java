package com.mallang.common.exception;

import java.util.Objects;

import org.springframework.http.HttpStatus;

public class GeneralException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public GeneralException(HttpStatus status, String message) {
        this(status, null, message);
    }

    public GeneralException(HttpStatus status, String code, String message) {
        super(message);
        this.status = Objects.requireNonNull(status, "HTTP status is required");
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
