package com.travel.meomulkyung.itinerary.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TourAPI 쉬는날 원문으로 방문일이 휴무일일 가능성을 판정한다.
 *
 * <p>확실히 읽히는 규칙만 판정한다: 요일, 요일 범위, 몇째 주 요일, 고정 날짜(M월 D일), 매월 D일.
 * 설·추석·공휴일·격주·계절 조건은 날짜를 확정할 수 없어 판정하지 않는다.
 * 판정 결과는 "휴무일일 수 있음"으로만 안내하고 단정하지 않는다.
 */
final class ClosedDayDetector {

    private static final Map<Character, DayOfWeek> DAYS = Map.of(
            '월', DayOfWeek.MONDAY, '화', DayOfWeek.TUESDAY, '수', DayOfWeek.WEDNESDAY, '목', DayOfWeek.THURSDAY,
            '금', DayOfWeek.FRIDAY, '토', DayOfWeek.SATURDAY, '일', DayOfWeek.SUNDAY);

    private static final Pattern BRACKETS = Pattern.compile("\\([^)]*\\)|\\[[^]]*]");
    /** "토·일요일", "월,화요일" → "토요일 일요일" */
    private static final Pattern DAY_LIST = Pattern.compile(
            "(?<!\\d)([월화수목금토일])\\s*[·,]\\s*(?=[월화수목금토일]\\s*[·,]?\\s*[월화수목금토일]?\\s*요일)");
    private static final Pattern SPLIT = Pattern.compile(
            "/|\\n|\\s및\\s|[·,](?!\\s*(?:\\d|첫|둘|두|셋|세|넷|네|다섯|마지막))");
    private static final Pattern NO_CLOSURE = Pattern.compile("연중\\s*무휴|휴무\\s*없음|휴관\\s*없음|^\\s*없음\\s*$|무휴");
    private static final Pattern UNDECIDABLE = Pattern.compile(
            "설날|설\\s*[·,및]|설\\s*(연휴|당일|전날|다음날)|추석|명절|공휴일|연휴|격주|하절기|동절기|성수기|비수기|시즌|임시|사정");
    private static final Pattern DAY_RANGE = Pattern.compile(
            "(?<!\\d)([월화수목금토일])(?:요일)?\\s*[~∼\\-–]\\s*([월화수목금토일])\\s*요일");
    private static final Pattern DAY = Pattern.compile("(?<!\\d)([월화수목금토일])\\s*요일");
    private static final Pattern FIXED_DATE = Pattern.compile("(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일");
    private static final Pattern MONTHLY_DATE = Pattern.compile("매월\\s*(\\d{1,2})\\s*일(?!\\s*요일)");
    private static final Pattern ORDINAL_WORD = Pattern.compile("(첫|둘|두|셋|세|넷|네|다섯)\\s*(?:번\\s*)?째|첫\\s*주|마지막");
    private static final Pattern ORDINAL_DIGIT = Pattern.compile("(\\d)(?=\\s*[·,]?\\s*\\d?\\s*(?:번째|째|주차|주))");

    private ClosedDayDetector() {
    }

    static boolean mayBeClosed(String restDate, LocalDate date) {
        if (restDate == null || restDate.isBlank() || date == null) {
            return false;
        }
        String normalized = DAY_LIST.matcher(BRACKETS.matcher(restDate).replaceAll(" ")).replaceAll("$1요일 ");
        for (String part : SPLIT.split(normalized)) {
            if (matches(part.trim(), date)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(String part, LocalDate date) {
        if (part.isEmpty() || NO_CLOSURE.matcher(part).find() || UNDECIDABLE.matcher(part).find()) {
            return false;
        }
        if (matchesFixedDate(part, date)) {
            return true;
        }
        Matcher monthly = MONTHLY_DATE.matcher(part);
        while (monthly.find()) {
            if (Integer.parseInt(monthly.group(1)) == date.getDayOfMonth()) {
                return true;
            }
        }
        List<DayOfWeek> days = weekdays(part);
        if (!days.contains(date.getDayOfWeek())) {
            return false;
        }
        List<Integer> ordinals = ordinals(part);
        return ordinals.isEmpty() || matchesOrdinal(ordinals, date);
    }

    private static boolean matchesFixedDate(String part, LocalDate date) {
        Matcher fixed = FIXED_DATE.matcher(part);
        while (fixed.find()) {
            if (Integer.parseInt(fixed.group(1)) == date.getMonthValue()
                    && Integer.parseInt(fixed.group(2)) == date.getDayOfMonth()) {
                return true;
            }
        }
        return false;
    }

    private static List<DayOfWeek> weekdays(String part) {
        List<DayOfWeek> days = new ArrayList<>();
        Matcher range = DAY_RANGE.matcher(part);
        String rest = part;
        while (range.find()) {
            DayOfWeek day = DAYS.get(range.group(1).charAt(0));
            DayOfWeek end = DAYS.get(range.group(2).charAt(0));
            for (int i = 0; i < 7; i++) {
                days.add(day);
                if (day == end) {
                    break;
                }
                day = day.plus(1);
            }
            rest = rest.replace(range.group(), " ");
        }
        Matcher single = DAY.matcher(rest);
        while (single.find()) {
            days.add(DAYS.get(single.group(1).charAt(0)));
        }
        return days;
    }

    /** 1~5는 N번째 주, 0은 마지막 주. */
    private static List<Integer> ordinals(String part) {
        List<Integer> ordinals = new ArrayList<>();
        Matcher word = ORDINAL_WORD.matcher(part);
        while (word.find()) {
            String value = word.group();
            if (value.startsWith("마지막")) ordinals.add(0);
            else if (value.startsWith("첫")) ordinals.add(1);
            else if (value.startsWith("둘") || value.startsWith("두")) ordinals.add(2);
            else if (value.startsWith("셋") || value.startsWith("세")) ordinals.add(3);
            else if (value.startsWith("넷") || value.startsWith("네")) ordinals.add(4);
            else ordinals.add(5);
        }
        Matcher digit = ORDINAL_DIGIT.matcher(part);
        while (digit.find()) {
            ordinals.add(Integer.parseInt(digit.group(1)));
        }
        return ordinals;
    }

    private static boolean matchesOrdinal(List<Integer> ordinals, LocalDate date) {
        int nth = (date.getDayOfMonth() - 1) / 7 + 1;
        boolean last = date.getDayOfMonth() + 7 > date.lengthOfMonth();
        return ordinals.contains(nth) || last && ordinals.contains(0);
    }
}
