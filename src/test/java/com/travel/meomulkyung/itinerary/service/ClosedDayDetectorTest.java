package com.travel.meomulkyung.itinerary.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ClosedDayDetectorTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 9, 21);

    @Test
    void fixtureDateIsMonday() {
        assertThat(MONDAY.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }

    @ParameterizedTest(name = "[{0}] {1} → {2}")
    @CsvSource(delimiter = '|', value = {
            // 매주 요일
            "매주 월요일                          | 2026-09-21 | true",
            "매주 월요일                          | 2026-09-22 | false",
            "매주 월요일(공휴일인 경우 다음날)      | 2026-09-21 | true",
            // 실측 원문 (영천 시안미술관, detailIntro2 restdateculture)
            "매주 월요일 (단, 월요일이 공휴일인 경우 익일 휴관) / 설·추석 당일 | 2026-09-21 | true",
            "매주 월요일 (단, 월요일이 공휴일인 경우 익일 휴관) / 설·추석 당일 | 2026-09-22 | false",
            "월요일 휴관                          | 2026-09-21 | true",
            // 요일 범위·목록
            "월요일~화요일                        | 2026-09-22 | true",
            "월요일~화요일                        | 2026-09-23 | false",
            "토·일요일                            | 2026-09-19 | true",
            "토·일요일                            | 2026-09-20 | true",
            "토·일요일                            | 2026-09-21 | false",
            // 몇째 주
            "매월 둘째, 넷째 월요일               | 2026-09-14 | true",
            "매월 둘째, 넷째 월요일               | 2026-09-28 | true",
            "매월 둘째, 넷째 월요일               | 2026-09-21 | false",
            "매월 2,4째 일요일                    | 2026-09-13 | true",
            "매월 2,4째 일요일                    | 2026-09-20 | false",
            "매월 마지막 주 화요일                | 2026-09-29 | true",
            "매월 마지막 주 화요일                | 2026-09-22 | false",
            // 고정 날짜·매월 날짜
            "1월 1일, 설날 및 추석 당일           | 2027-01-01 | true",
            "매월 15일                            | 2026-09-15 | true",
            "매월 15일                            | 2026-09-16 | false",
            // 판정하지 않음
            "1월 1일, 설날 및 추석 당일           | 2026-09-25 | false",
            "공휴일                               | 2026-09-21 | false",
            "격주 월요일                          | 2026-09-21 | false",
            "연중무휴                             | 2026-09-21 | false",
            "없음                                 | 2026-09-21 | false",
            "시설 점검일 별도 공지                | 2026-09-21 | false"
    })
    void detectsOnlyRulesItCanReadWithConfidence(String restDate, LocalDate date, boolean expected) {
        assertThat(ClosedDayDetector.mayBeClosed(restDate.trim(), date)).isEqualTo(expected);
    }

    @Test
    void multilineRestDateIsSplitIntoRules() {
        assertThat(ClosedDayDetector.mayBeClosed("1월 1일\n매주 월요일", MONDAY)).isTrue();
    }

    @Test
    void blankInputIsNotClosed() {
        assertThat(ClosedDayDetector.mayBeClosed(null, MONDAY)).isFalse();
        assertThat(ClosedDayDetector.mayBeClosed(" ", MONDAY)).isFalse();
    }
}
