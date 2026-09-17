package com.travel.meomulkyung.itinerary.domain;

import java.util.Locale;
import java.util.Optional;

/**
 * 일정 구간 이동수단.
 *
 * <p>도보는 선택지로 두지 않는다. 경북 군 단위 장소 간 거리가 수~수십 km라
 * 도보 기준 일정은 의미가 없기 때문이다.
 */
public enum TransportMode {
    CAR,
    TRANSIT;

    public static Optional<TransportMode> from(String value) {
        if (value == null || value.isBlank()) {
            return Optional.of(CAR);
        }
        try {
            return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
