package com.travel.meomulkyung.mypage.controller;

import com.travel.meomulkyung.mypage.service.MyPageException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 마이페이지 예외 → JSON 응답 매핑. 일정 도메인과 동일한 {status, code, message} 형태를 쓴다. */
@RestControllerAdvice
public class MyPageExceptionHandler {

    record Error(int status, String code, String message) {
    }

    @ExceptionHandler(MyPageException.class)
    ResponseEntity<Error> myPage(MyPageException exception) {
        return ResponseEntity.status(exception.status)
                .body(new Error(exception.status.value(), exception.code, exception.getMessage()));
    }
}
