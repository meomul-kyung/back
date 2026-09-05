package com.travel.meomulkyung.recommendation.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PreferenceTag {
    HANOK_CONFUCIANISM("HANOK_CONFUCIANISM", "\uD55C\uC625\u00B7\uC720\uAD50"),
    NATURE("NATURE", "\uC790\uC5F0"),
    SEA("SEA", "\uBC14\uB2E4"),
    WALKING("WALKING", "\uAC77\uAE30"),
    HEALING("HEALING", "\uD790\uB9C1"),
    FOOD("FOOD", "\uC74C\uC2DD"),
    BICYCLE("BICYCLE", "\uC790\uC804\uAC70"),
    HISTORY("HISTORY", "\uC5ED\uC0AC"),
    NIGHT_SKY("NIGHT_SKY", "\uBCC4\u00B7\uBC24\uD558\uB298");

    private final String code;
    private final String label;
}
