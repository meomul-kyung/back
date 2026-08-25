package com.travel.meomulkyung.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 공통 예외 → JSON 응답 매핑.
 * IllegalArgumentException → 400, IllegalStateException → 409.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(errorBody(e, "잘못된 요청입니다."));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody(e, "요청을 처리할 수 없습니다."));
    }

    private Map<String, String> errorBody(Exception e, String fallback) {
        String message = (e.getMessage() != null) ? e.getMessage() : fallback;
        return Map.of("message", message);
    }
}
