package com.travel.meomulkyung.recommendation.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 「지역별 관광 수요 강도」 체류 강도(areaTarSjrnDsList) 기반 구현.
 *
 * <p>{@code signguCd}를 생략하고 {@code areaCd}만 주면 경상북도 전 시군구가 한 번에 오므로,
 * 지역 수와 무관하게 <b>지표 코드당 1회</b>만 호출한다. 1박·2박·3박 이상 세 지표를 쓰므로
 * 기준 연월이 같으면 총 3회다. 그마저도 기준 연월 단위로 캐시하므로 애플리케이션이 떠 있는
 * 동안 사실상 3회로 끝난다.
 *
 * <p>응답에는 {@code signguCd}가 {@code "0"}인 도 전체 집계 행이 섞여 온다. 이 행은 시군구와
 * 산식이 달라 함께 정규화하면 값이 왜곡되므로, 설정에 등록된 시군구 코드만 받아들인다.
 *
 * <p>조회에 실패하면 예외를 던지지 않고 빈 Map을 돌려준다. 추천은 외부 API 없이도 동작해야
 * 하는 기능이고, 수요 강도는 순위를 다듬는 부가 근거이기 때문이다.
 */
public class TourApiDemandProvider implements TourDemandProvider {

    private static final Logger log = LoggerFactory.getLogger(TourApiDemandProvider.class);

    /** 숙박일수별 체류 강도 지표 코드. 순서대로 1박 / 2박 / 3박 이상. */
    private static final String ONE_NIGHT = "2103";
    private static final String TWO_NIGHTS = "2104";
    private static final String THREE_OR_MORE_NIGHTS = "2105";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final TourDemandProperties properties;
    /** 기준 연월 → 지역별 체류 강도. 월 1회 갱신 데이터라 오래 들고 있어도 무방하다. */
    private final Map<String, Map<Long, StayProfile>> cache = new ConcurrentHashMap<>();

    public TourApiDemandProvider(RestClient restClient, ObjectMapper objectMapper, TourDemandProperties properties) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public Map<Long, StayProfile> findStayProfiles() {
        if (!StringUtils.hasText(properties.getServiceKey())
                || !StringUtils.hasText(properties.getAreaCd())
                || properties.getSignguCodes().isEmpty()) {
            return Map.of();
        }
        return cache.computeIfAbsent(properties.getBaseYm(), ignored -> fetchStayProfiles());
    }

    private Map<Long, StayProfile> fetchStayProfiles() {
        Map<Long, Double> oneNight = fetchIndicator(ONE_NIGHT);
        Map<Long, Double> twoNights = fetchIndicator(TWO_NIGHTS);
        Map<Long, Double> threeOrMore = fetchIndicator(THREE_OR_MORE_NIGHTS);

        Map<Long, StayProfile> profiles = new HashMap<>();
        properties.getSignguCodes().keySet().forEach(regionId -> {
            Double first = oneNight.get(regionId);
            Double second = twoNights.get(regionId);
            Double third = threeOrMore.get(regionId);
            // 세 지표가 모두 있어야 어떤 박수로 조회하든 같은 기준으로 비교할 수 있다.
            if (first != null && second != null && third != null) {
                profiles.put(regionId, new StayProfile(first, second, third));
            }
        });
        return Map.copyOf(profiles);
    }

    /** 지표 하나를 경상북도 전체로 한 번 조회해 {@code region_id}별 값으로 옮긴다. */
    private Map<Long, Double> fetchIndicator(String indicatorCode) {
        try {
            String response = restClient.get().uri(requestUri(indicatorCode)).retrieve().body(String.class);
            JsonNode root = objectMapper.readTree(response).path("response");
            String resultCode = root.path("header").path("resultCode").asText();
            if (!"0000".equals(resultCode)) {
                log.warn("지역별 관광 수요 강도가 오류 코드를 반환했습니다. indicator={}, resultCode={}, resultMsg={}",
                        indicatorCode, resultCode, root.path("header").path("resultMsg").asText());
                return Map.of();
            }

            Map<String, Long> regionIdBySignguCd = new HashMap<>();
            properties.getSignguCodes().forEach((regionId, signguCd) -> regionIdBySignguCd.put(signguCd, regionId));

            Map<Long, Double> values = new HashMap<>();
            JsonNode items = root.path("body").path("items").path("item");
            for (JsonNode item : items.isArray() ? items : objectMapper.createArrayNode().add(items)) {
                Long regionId = regionIdBySignguCd.get(item.path("signguCd").asText(null));
                Double value = number(item.path("tarSjrnDsIxVal"));
                if (regionId != null && value != null) {
                    values.put(regionId, value);
                }
            }
            return values;
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException exception) {
            log.warn("지역별 관광 수요 강도 조회에 실패해 체류 강도를 반영하지 않습니다. indicator={}, reason={}",
                    indicatorCode, exception.getMessage());
            return Map.of();
        }
    }

    private URI requestUri(String indicatorCode) {
        String uri = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .pathSegment("areaTarSjrnDsList")
                .queryParam("MobileOS", properties.getMobileOs())
                .queryParam("MobileApp", properties.getMobileApp())
                .queryParam("_type", "json")
                .queryParam("pageNo", 1)
                // 경상북도 시군구는 23개이고 도 전체 행이 하나 더 붙는다. 넉넉히 잡아 한 번에 받는다.
                .queryParam("numOfRows", 300)
                .queryParam("baseYm", properties.getBaseYm())
                .queryParam("areaCd", properties.getAreaCd())
                .queryParam("tarSjrnDsIxCd", indicatorCode)
                .build()
                .encode()
                .toUriString();
        // 공공데이터포털 Encoding 키는 이미 퍼센트 인코딩돼 있어 그대로 붙여야 한다.
        // queryParam()으로 넘기면 %2B가 %252B로 이중 인코딩되어 인증이 실패한다.
        return URI.create(uri + "&serviceKey=" + properties.getServiceKey());
    }

    private static Double number(JsonNode value) {
        if (value.isMissingNode() || value.isNull() || !StringUtils.hasText(value.asText())) {
            return null;
        }
        try {
            return Double.parseDouble(value.asText());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /** 테스트 편의를 위해 캐시를 비운다. */
    void clearCache() {
        cache.clear();
    }
}
