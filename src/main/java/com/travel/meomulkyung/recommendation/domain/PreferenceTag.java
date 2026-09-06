package com.travel.meomulkyung.recommendation.domain;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
@Getter
@RequiredArgsConstructor
public enum PreferenceTag {
    HANOK_CONFUCIANISM("HANOK_CONFUCIANISM", "한옥·유교"), NATURE("NATURE", "자연"), SEA("SEA", "바다"), WALKING("WALKING", "걷기"), HEALING("HEALING", "힐링"), FOOD("FOOD", "음식"), BICYCLE("BICYCLE", "자전거"), HISTORY("HISTORY", "역사"), NIGHT_SKY("NIGHT_SKY", "별·밤하늘");
    private final String code;
    private final String label;
}
