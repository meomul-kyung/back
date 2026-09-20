package com.travel.meomulkyung.region.service;

import org.springframework.http.HttpStatus;

/** 지역 갤러리 조회 중 발생하는 예외. */
public class RegionGalleryException extends RuntimeException {

    public final HttpStatus status;
    public final String code;

    public RegionGalleryException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
