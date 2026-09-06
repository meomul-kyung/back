package com.travel.meomulkyung.recommendation.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CompanionType {

    SOLO("SOLO", "혼자"),
    COUPLE("COUPLE", "연인"),
    FRIENDS("FRIENDS", "친구"),
    FAMILY("FAMILY", "가족");

    private final String code;
    private final String label;
}
