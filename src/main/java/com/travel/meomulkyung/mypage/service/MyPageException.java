package com.travel.meomulkyung.mypage.service;

import org.springframework.http.HttpStatus;

/** 마이페이지 조회 중 발생하는 예외. */
public class MyPageException extends RuntimeException {

    public final HttpStatus status;
    public final String code;

    public MyPageException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
