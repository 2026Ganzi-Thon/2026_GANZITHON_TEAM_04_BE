package com.mallang.common.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ExceptionAdvice extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ExceptionAdvice.class);

    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<ProblemDetail> handleGeneralException(GeneralException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(error(exception.getStatus(), exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException exception) {
        return ResponseEntity.badRequest()
                .body(error(HttpStatus.BAD_REQUEST, null, "잘못된 요청입니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(Exception exception) {
        log.error("Unhandled request exception: {}", exception.getClass().getName());
        return ResponseEntity.internalServerError()
                .body(error(HttpStatus.INTERNAL_SERVER_ERROR, null, "서버 오류가 발생했습니다."));
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers,
                                                           HttpStatusCode status, WebRequest request) {
        return super.createResponseEntity(error(status, null, "잘못된 요청입니다."), headers, status, request);
    }

    private ProblemDetail error(HttpStatusCode status, String code, String message) {
        if (code == null) {
            code = switch (status.value()) {
                case 401 -> "UNAUTHORIZED";
                case 403 -> "FORBIDDEN";
                case 404 -> "NOT_FOUND";
                case 409 -> "INVALID_STATE";
                default -> status.is5xxServerError() ? "INTERNAL_ERROR" : "INVALID_INPUT";
            };
        }
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setProperty("success", false);
        detail.setProperty("error", Map.of("code", code, "message", message));
        return detail;
    }
}
