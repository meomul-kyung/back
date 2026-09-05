package com.travel.meomulkyung.recommendation.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CompanionType {
    SOLO("SOLO", "\uD63C\uC790"),
    COUPLE("COUPLE", "\uC5F0\uC778"),
    FRIENDS("FRIENDS", "\uCE5C\uAD6C"),
    FAMILY("FAMILY", "\uAC00\uC871");

    private final String code;
    private final String label;
}
