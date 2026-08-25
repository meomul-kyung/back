package com.travel.meomulkyung.user.dto;

/** 개인정보(이름·닉네임) 수정 요청. 전달된 필드만 반영된다. */
public record ProfileUpdateRequest(String name, String nickname) {
}
