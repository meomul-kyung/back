package com.travel.meomulkyung.itinerary.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.meomulkyung.region.domain.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 기상청 중기예보(공공데이터포털) 기반 날씨 조회. 단기예보가 닿지 않는 뒷날짜를 채운다.
 *
 * <p><b>예보 범위는 발표일 기준</b>이며 발표 회차에 따라 다르다.
 * 06시 발표는 4일 후 ~ 10일 후, 18시 발표는 5일 후 ~ 10일 후를 제공한다.
 * 오프셋이 "오늘"이 아니라 "발표일"을 기준으로 세어지므로, 어제 18시 발표의 5일 후는
 * 오늘의 4일 후와 같다. 덕분에 단기예보(오늘 ~ 3일 후)와 사이에 빈 날짜가 생기지 않는다.
 *
 * <p>두 API를 함께 쓴다. 날씨 상태는 광역 단위인 중기육상예보(getMidLandFcst)에서,
 * 최저·최고기온은 시군 단위인 중기기온(getMidTa)에서 가져온다. 육상예보는 운영 지역 15곳이
 * 모두 같은 구역코드(경상북도)를 쓰므로 발표 회차당 한 번만 호출하고 공유한다.
 *
 * <p>4~7일 후는 오전·오후가 나뉘어 오고 8~10일 후는 하루 한 값만 온다.
 * 하루에 하나만 노출하는 화면이라 <b>악천후 우선</b>으로 합친다. 오전에 비가 와도 오후가
 * 맑으면 비 예보가 사라지는 편이 여행 계획에 더 나쁘기 때문이다.
 *
 * <p>날씨는 일정 조회의 부가 정보다. 호출이 실패하거나 설정이 비어 있으면 예외를 던지지 않고
 * {@code available=false}를 돌려준다.
 */
public class MidTermWeatherProvider implements WeatherProvider {

    private static final Logger log = LoggerFactory.getLogger(MidTermWeatherProvider.class);

    private static final Weather UNAVAILABLE = new Weather(false, null, null, null, null);
    private static final DateTimeFormatter TM_FC_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    /** 발표 후 자료가 올라올 때까지의 여유. */
    private static final int ANNOUNCEMENT_DELAY_MINUTES = 30;
    /** 오전·오후가 나뉘어 오는 마지막 일자. 8일 후부터는 하루 한 값이다. */
    private static final int LAST_SPLIT_DAY = 7;
    /** 악천후 우선 비교용 심각도. 뒤로 갈수록 나쁜 날씨. */
    private static final List<String> SEVERITY =
            List.of("SUNNY", "PARTLY_CLOUDY", "CLOUDY", "SHOWER", "RAIN", "SLEET", "SNOW");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final MidTermWeatherProperties properties;
    private final Clock clock;
    /** 발표 회차 → 날짜별 날씨 아이콘. 지역과 무관하게 공유한다. */
    private final Map<String, Map<LocalDate, String>> landCache = new ConcurrentHashMap<>();
    /** 지역+발표 회차 → 날짜별 {최저, 최고} 기온. */
    private final Map<String, Map<LocalDate, int[]>> temperatureCache = new ConcurrentHashMap<>();

    public MidTermWeatherProvider(RestClient restClient, ObjectMapper objectMapper,
                                  MidTermWeatherProperties properties, Clock clock) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public Weather weather(Region region, LocalDate date) {
        String temperatureRegionId = properties.getTemperatureRegionIds().get(region.getId());
        if (!StringUtils.hasText(temperatureRegionId)
                || !StringUtils.hasText(properties.getServiceKey())
                || !StringUtils.hasText(properties.getLandRegionId())) {
            return UNAVAILABLE;
        }

        Announcement announcement = latestAnnouncement(LocalDateTime.now(clock));
        LocalDate first = announcement.date().plusDays(announcement.firstForecastDay());
        LocalDate last = announcement.date().plusDays(properties.getLastForecastDay());
        if (date.isBefore(first) || date.isAfter(last)) {
            return UNAVAILABLE;
        }

        evictOtherAnnouncements(announcement);
        Map<LocalDate, int[]> temperatures = temperatureCache.computeIfAbsent(
                temperatureRegionId + "|" + announcement.key(),
                ignored -> fetchTemperatures(temperatureRegionId, announcement));
        int[] temperature = temperatures.get(date);
        if (temperature == null) {
            return UNAVAILABLE;
        }

        Map<LocalDate, String> icons = landCache.computeIfAbsent(
                announcement.key(), ignored -> fetchLand(announcement));
        return new Weather(true, icons.get(date), temperature[1], temperature[0], temperature[1]);
    }

    /**
     * 요청 시점 기준 가장 최근 발표 회차. 중기예보는 06시·18시 하루 두 번 발표된다.
     * 06시 전이면 전날 18시 발표가 최신이다.
     */
    static Announcement latestAnnouncement(LocalDateTime now) {
        LocalDateTime adjusted = now.minusMinutes(ANNOUNCEMENT_DELAY_MINUTES);
        if (adjusted.getHour() >= 18) {
            return new Announcement(adjusted.toLocalDate(), "1800");
        }
        if (adjusted.getHour() >= 6) {
            return new Announcement(adjusted.toLocalDate(), "0600");
        }
        return new Announcement(adjusted.toLocalDate().minusDays(1), "1800");
    }

    private void evictOtherAnnouncements(Announcement announcement) {
        landCache.keySet().removeIf(key -> !key.equals(announcement.key()));
        temperatureCache.keySet().removeIf(key -> !key.endsWith("|" + announcement.key()));
    }

    private Map<LocalDate, String> fetchLand(Announcement announcement) {
        JsonNode item = fetchItem("getMidLandFcst", properties.getLandRegionId(), announcement);
        if (item == null) {
            return Map.of();
        }
        Map<LocalDate, String> icons = new HashMap<>();
        for (int day = announcement.firstForecastDay(); day <= properties.getLastForecastDay(); day++) {
            String icon = day <= LAST_SPLIT_DAY
                    ? worse(text(item, "wf" + day + "Am"), text(item, "wf" + day + "Pm"))
                    : icon(text(item, "wf" + day));
            if (icon != null) {
                icons.put(announcement.date().plusDays(day), icon);
            }
        }
        return icons;
    }

    private Map<LocalDate, int[]> fetchTemperatures(String regionId, Announcement announcement) {
        JsonNode item = fetchItem("getMidTa", regionId, announcement);
        if (item == null) {
            return Map.of();
        }
        Map<LocalDate, int[]> temperatures = new HashMap<>();
        for (int day = announcement.firstForecastDay(); day <= properties.getLastForecastDay(); day++) {
            Integer minimum = number(item, "taMin" + day);
            Integer maximum = number(item, "taMax" + day);
            if (minimum == null && maximum == null) {
                continue;
            }
            int low = minimum != null ? minimum : maximum;
            int high = maximum != null ? maximum : minimum;
            temperatures.put(announcement.date().plusDays(day), new int[]{low, high});
        }
        return temperatures;
    }

    private JsonNode fetchItem(String operation, String regionId, Announcement announcement) {
        try {
            String response = restClient.get()
                    .uri(requestUri(operation, regionId, announcement))
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(response).path("response");
            String resultCode = root.path("header").path("resultCode").asText();
            if (!"00".equals(resultCode)) {
                log.warn("기상청 중기예보가 오류 코드를 반환했습니다. operation={}, resultCode={}, resultMsg={}",
                        operation, resultCode, root.path("header").path("resultMsg").asText());
                return null;
            }
            JsonNode items = root.path("body").path("items").path("item");
            JsonNode item = items.isArray() ? items.path(0) : items;
            return item.isObject() ? item : null;
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException exception) {
            log.warn("기상청 중기예보 조회에 실패해 날씨를 제공하지 않습니다. operation={}, reason={}",
                    operation, exception.getMessage());
            return null;
        }
    }

    private URI requestUri(String operation, String regionId, Announcement announcement) {
        String uri = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .pathSegment(operation)
                .queryParam("dataType", "JSON")
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 10)
                .queryParam("regId", regionId)
                .queryParam("tmFc", announcement.key())
                .build()
                .encode()
                .toUriString();
        // 공공데이터포털 Encoding 키는 이미 퍼센트 인코딩돼 있어 그대로 붙여야 한다.
        // queryParam()으로 넘기면 %2B가 %252B로 이중 인코딩되어 인증이 실패한다.
        return URI.create(uri + "&serviceKey=" + properties.getServiceKey());
    }

    /** 오전·오후 예보 중 더 나쁜 쪽을 고른다. */
    private static String worse(String morning, String afternoon) {
        String first = icon(morning);
        String second = icon(afternoon);
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return SEVERITY.indexOf(second) > SEVERITY.indexOf(first) ? second : first;
    }

    /**
     * 중기예보 날씨 문자열을 단기예보와 같은 아이콘 코드로 옮긴다.
     * 프론트가 이미 쓰는 어휘를 그대로 쓰므로 화면 수정이 필요 없다.
     *
     * <p>검사 순서가 중요하다. "흐리고 비/눈"은 "비"도 "눈"도 포함하므로
     * 복합 표현을 먼저 걸러야 한다.
     */
    static String icon(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        if (text.contains("비/눈") || text.contains("눈/비")) {
            return "SLEET";
        }
        if (text.contains("소나기")) {
            return "SHOWER";
        }
        if (text.contains("눈")) {
            return "SNOW";
        }
        if (text.contains("비")) {
            return "RAIN";
        }
        if (text.contains("흐림")) {
            return "CLOUDY";
        }
        if (text.contains("구름많음")) {
            return "PARTLY_CLOUDY";
        }
        if (text.contains("맑음")) {
            return "SUNNY";
        }
        return null;
    }

    private static String text(JsonNode item, String field) {
        JsonNode value = item.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private static Integer number(JsonNode item, String field) {
        JsonNode value = item.path(field);
        if (value.isMissingNode() || value.isNull() || !StringUtils.hasText(value.asText())) {
            return null;
        }
        try {
            return (int) Math.round(Double.parseDouble(value.asText()));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /** 테스트 편의를 위해 캐시를 비운다. */
    void clearCache() {
        landCache.clear();
        temperatureCache.clear();
    }

    /**
     * 중기예보 발표 회차. {@code tmFc} 파라미터는 {@code yyyyMMddHHmm} 형식이다.
     */
    record Announcement(LocalDate date, String time) {

        String key() {
            return date.format(TM_FC_DATE) + time;
        }

        /** 06시 발표는 4일 후부터, 18시 발표는 5일 후부터 제공된다. */
        int firstForecastDay() {
            return "0600".equals(time) ? 4 : 5;
        }
    }
}
