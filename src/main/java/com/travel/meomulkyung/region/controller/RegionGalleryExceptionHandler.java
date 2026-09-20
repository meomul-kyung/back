package com.travel.meomulkyung.region.controller;

import com.travel.meomulkyung.region.service.RegionGalleryException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 지역 갤러리 예외 → JSON 응답 매핑. 다른 도메인과 동일한 {status, code, message} 형태를 쓴다. */
@RestControllerAdvice
public class RegionGalleryExceptionHandler {

    record Error(int status, String code, String message) {
    }

    @ExceptionHandler(RegionGalleryException.class)
    ResponseEntity<Error> regionGallery(RegionGalleryException exception) {
        return ResponseEntity.status(exception.status)
                .body(new Error(exception.status.value(), exception.code, exception.getMessage()));
    }
}
