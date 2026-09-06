package com.travel.meomulkyung.global.exception;

import com.travel.meomulkyung.region.exception.RegionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(RegionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRegionNotFound(RegionNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("REGION_NOT_FOUND", exception.getMessage()));
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(errorBody(exception, "잘못된 요청입니다."));
    }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody(exception, "요청을 처리할 수 없습니다."));
    }
    private Map<String, String> errorBody(Exception exception, String fallback) {
        return Map.of("message", exception.getMessage() != null ? exception.getMessage() : fallback);
    }
    public record ErrorResponse(String code, String message) { }
}
