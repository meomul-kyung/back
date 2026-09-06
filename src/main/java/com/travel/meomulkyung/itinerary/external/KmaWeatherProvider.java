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
 * 기상청 API허브(apihub.kma.go.kr) 단기예보(getVilageFcst) 기반 날씨 조회.
 *
 * <p>TourAPI는 공공데이터포털의 {@code serviceKey}를 쓰지만 이쪽은 API허브의 {@code authKey}를 쓴다.
 * 두 연동은 호스트·인증 파라미터가 서로 다르므로 설정과 클라이언트를 분리해 둔다.
 *
 * <p>단기예보는 하루 8회(02·05·08·11·14·17·20·23시) 발표되며 발표 시점으로부터 약 3일치를 제공한다.
 * 한 번의 호출이 여러 날짜를 담아 오므로, 발표 회차 단위로 결과를 캐시해 일정의 날짜 수만큼
 * 반복 호출하지 않는다.
 *
 * <p>날씨는 일정 조회의 부가 정보이므로, 호출이 실패하거나 설정이 비어 있으면 예외를 던지지 않고
 * {@code available=false}를 돌려준다. 날씨 때문에 일정 조회 자체가 실패해서는 안 된다.
 */
public class KmaWeatherProvider implements WeatherProvider {

    private static final Logger log = LoggerFactory.getLogger(KmaWeatherProvider.class);

    private static final Weather UNAVAILABLE = new Weather(false, null, null, null, null);
    private static final DateTimeFormatter BASE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    /** 발표 시각(정시). 각 회차는 발표 후 약 10분 뒤부터 제공된다. */
    private static final int[] ANNOUNCEMENT_HOURS = {23, 20, 17, 14, 11, 8, 5, 2};
    /** 발표 직후 데이터가 아직 안 올라오는 구간을 피하기 위한 여유. */
    private static final int ANNOUNCEMENT_DELAY_MINUTES = 45;
    /** 하루를 대표하는 시각. 이 시각의 하늘상태·기온을 그 날의 값으로 쓴다. */
    private static final String REPRESENTATIVE_TIME = "1200";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final KmaWeatherProperties properties;
    private final Clock clock;
    private final Map<String, Map<LocalDate, Weather>> cache = new ConcurrentHashMap<>();

    public KmaWeatherProvider(RestClient restClient, ObjectMapper objectMapper,
                              KmaWeatherProperties properties, Clock clock) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public Weather weather(Region region, LocalDate date) {
        KmaWeatherProperties.Coordinate coordinate = properties.getCoordinates().get(region.getId());
        if (coordinate == null || coordinate.getLatitude() == null || coordinate.getLongitude() == null) {
            return UNAVAILABLE;
        }
        if (!StringUtils.hasText(properties.getAuthKey())) {
            return UNAVAILABLE;
        }

        LocalDate today = LocalDate.now(clock);
        if (date.isBefore(today) || date.isAfter(today.plusDays(properties.getForecastDays()))) {
            return UNAVAILABLE;
        }

        Announcement announcement = latestAnnouncement(LocalDateTime.now(clock));
        String key = region.getId() + "|" + announcement.date().format(BASE_DATE) + announcement.time();
        evictOtherAnnouncements(announcement);

        Map<LocalDate, Weather> forecast = cache.computeIfAbsent(key,
                ignored -> fetch(coordinate, announcement));
        return forecast.getOrDefault(date, UNAVAILABLE);
    }

    /** 요청 시점 기준 가장 최근 발표 회차. 발표 직후 여유 시간을 빼고 계산한다. */
    static Announcement latestAnnouncement(LocalDateTime now) {
        LocalDateTime adjusted = now.minusMinutes(ANNOUNCEMENT_DELAY_MINUTES);
        for (int hour : ANNOUNCEMENT_HOURS) {
            if (adjusted.getHour() >= hour) {
                return new Announcement(adjusted.toLocalDate(), String.format("%02d00", hour));
            }
        }
        return new Announcement(adjusted.toLocalDate().minusDays(1), "2300");
    }

    private void evictOtherAnnouncements(Announcement announcement) {
        String suffix = "|" + announcement.date().format(BASE_DATE) + announcement.time();
        cache.keySet().removeIf(key -> !key.endsWith(suffix));
    }

    private Map<LocalDate, Weather> fetch(KmaWeatherProperties.Coordinate coordinate, Announcement announcement) {
        try {
            KmaGridConverter.Grid grid =
                    KmaGridConverter.toGrid(coordinate.getLatitude(), coordinate.getLongitude());
            String response = restClient.get().uri(requestUri(grid, announcement)).retrieve().body(String.class);
            return parse(response);
        } catch (RuntimeException exception) {
            log.warn("기상청 단기예보 조회에 실패해 날씨를 제공하지 않습니다. reason={}", exception.getMessage());
            return Map.of();
        }
    }

    private URI requestUri(KmaGridConverter.Grid grid, Announcement announcement) {
        // API허브 authKey는 URL 안전 문자만 쓰므로 일반 쿼리 파라미터로 넘긴다.
        return URI.create(UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .pathSegment("getVilageFcst")
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", properties.getPageSize())
                .queryParam("dataType", "JSON")
                .queryParam("base_date", announcement.date().format(BASE_DATE))
                .queryParam("base_time", announcement.time())
                .queryParam("nx", grid.nx())
                .queryParam("ny", grid.ny())
                .queryParam("authKey", properties.getAuthKey())
                .build()
                .encode()
                .toUriString());
    }

    private Map<LocalDate, Weather> parse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode body = root.path("response");
            String resultCode = body.path("header").path("resultCode").asText();
            if (!"00".equals(resultCode)) {
                log.warn("기상청 단기예보가 오류 코드를 반환했습니다. resultCode={}", resultCode);
                return Map.of();
            }

            JsonNode items = body.path("body").path("items").path("item");
            if (!items.isArray()) {
                return Map.of();
            }

            Map<LocalDate, DailyForecast> byDate = new HashMap<>();
            for (JsonNode item : items) {
                String fcstDate = item.path("fcstDate").asText(null);
                if (!StringUtils.hasText(fcstDate)) {
                    continue;
                }
                LocalDate date = LocalDate.parse(fcstDate, BASE_DATE);
                byDate.computeIfAbsent(date, ignored -> new DailyForecast())
                        .accept(item.path("category").asText(null),
                                item.path("fcstTime").asText(null),
                                item.path("fcstValue").asText(null));
            }

            Map<LocalDate, Weather> forecast = new HashMap<>();
            byDate.forEach((date, daily) -> forecast.put(date, daily.toWeather()));
            return forecast;
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException exception) {
            log.warn("기상청 단기예보 응답을 해석하지 못했습니다. reason={}", exception.getMessage());
            return Map.of();
        }
    }

    record Announcement(LocalDate date, String time) {
    }

    /** 3시간 간격 예보를 하루 단위로 접는다. */
    private static final class DailyForecast {
        private Integer minimumTemperature;
        private Integer maximumTemperature;
        private Integer representativeTemperature;
        private Integer fallbackTemperature;
        private String sky;
        private String precipitation;
        private String fallbackSky;
        private String fallbackPrecipitation;

        void accept(String category, String time, String value) {
            if (category == null || value == null) {
                return;
            }
            boolean representative = REPRESENTATIVE_TIME.equals(time);
            switch (category) {
                case "TMN" -> minimumTemperature = toInteger(value);
                case "TMX" -> maximumTemperature = toInteger(value);
                case "TMP" -> {
                    if (representative) {
                        representativeTemperature = toInteger(value);
                    } else if (fallbackTemperature == null) {
                        fallbackTemperature = toInteger(value);
                    }
                }
                case "SKY" -> {
                    if (representative) {
                        sky = value;
                    } else if (fallbackSky == null) {
                        fallbackSky = value;
                    }
                }
                case "PTY" -> {
                    if (representative) {
                        precipitation = value;
                    } else if (fallbackPrecipitation == null) {
                        fallbackPrecipitation = value;
                    }
                }
                default -> { }
            }
        }

        Weather toWeather() {
            Integer temperature = representativeTemperature != null ? representativeTemperature : fallbackTemperature;
            Integer minimum = minimumTemperature != null ? minimumTemperature : temperature;
            Integer maximum = maximumTemperature != null ? maximumTemperature : temperature;
            String icon = icon(sky != null ? sky : fallbackSky,
                    precipitation != null ? precipitation : fallbackPrecipitation);
            return new Weather(true, icon, temperature, minimum, maximum);
        }

        private String icon(String sky, String precipitation) {
            if (precipitation != null && !"0".equals(precipitation)) {
                return switch (precipitation) {
                    case "1" -> "RAIN";
                    case "2" -> "SLEET";
                    case "3" -> "SNOW";
                    case "4" -> "SHOWER";
                    default -> "RAIN";
                };
            }
            if (sky == null) {
                return null;
            }
            return switch (sky) {
                case "1" -> "SUNNY";
                case "3" -> "PARTLY_CLOUDY";
                case "4" -> "CLOUDY";
                default -> null;
            };
        }

        private Integer toInteger(String value) {
            try {
                return (int) Math.round(Double.parseDouble(value));
            } catch (NumberFormatException exception) {
                return null;
            }
        }
    }

    /** 테스트 편의를 위해 캐시를 비운다. */
    void clearCache() {
        cache.clear();
    }

    static List<String> supportedIcons() {
        return List.of("SUNNY", "PARTLY_CLOUDY", "CLOUDY", "RAIN", "SLEET", "SNOW", "SHOWER");
    }
}
