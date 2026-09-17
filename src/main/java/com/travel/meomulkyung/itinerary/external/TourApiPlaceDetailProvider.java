package com.travel.meomulkyung.itinerary.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

/**
 * TourAPI(KorService2) 상세 조회. 국문 관광정보 서비스 안의 기능이라 별도 활용신청이 필요 없다.
 *
 * <p>타입별 필드 이름이 다르다. 실측(안동·영양 46건) 기준 관광지·문화시설·음식점만 값이 채워져 있어 세 타입만 지원한다.
 */
public class TourApiPlaceDetailProvider implements PlaceDetailProvider {

    /** contentTypeId → {이용시간 필드, 쉬는날 필드} */
    static final Map<String, String[]> FIELDS = Map.of(
            "12", new String[]{"usetime", "restdate"},
            "14", new String[]{"usetimeculture", "restdateculture"},
            "39", new String[]{"opentimefood", "restdatefood"});

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final TourApiProperties properties;

    public TourApiPlaceDetailProvider(RestClient restClient, ObjectMapper objectMapper, TourApiProperties properties) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public static boolean supports(String contentTypeId) {
        return contentTypeId != null && FIELDS.containsKey(contentTypeId);
    }

    @Override
    public Optional<String> contentTypeId(long contentId) {
        JsonNode item = firstItem(get("detailCommon2", Map.of("contentId", String.valueOf(contentId))));
        if (item == null) {
            return Optional.empty();
        }
        String value = item.path("contenttypeid").asText(null);
        return StringUtils.hasText(value) ? Optional.of(value.trim()) : Optional.empty();
    }

    @Override
    public OperationHours operationHours(long contentId, String contentTypeId) {
        String[] fields = FIELDS.get(contentTypeId);
        if (fields == null) {
            return new OperationHours(null, null);
        }
        JsonNode item = firstItem(get("detailIntro2",
                Map.of("contentId", String.valueOf(contentId), "contentTypeId", contentTypeId)));
        if (item == null) {
            return new OperationHours(null, null);
        }
        return new OperationHours(clean(item.path(fields[0]).asText(null)), clean(item.path(fields[1]).asText(null)));
    }

    /** {@code <br>}은 줄바꿈으로, 나머지 태그는 제거하고 흔한 HTML 엔티티만 되돌린다. */
    static String clean(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String text = raw.replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("<[^>]+>", "")
                .replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replaceAll("[ \\t]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{2,}", "\n")
                .trim();
        return text.isEmpty() ? null : text;
    }

    private String get(String operation, Map<String, String> params) {
        if (!StringUtils.hasText(properties.getServiceKey())) {
            throw new TourApiProviderException("TourAPI service key is not configured.");
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .pathSegment(operation)
                .queryParam("MobileOS", properties.getMobileOs())
                .queryParam("MobileApp", properties.getMobileApp())
                .queryParam("_type", "json");
        params.forEach(builder::queryParam);
        String uri = builder.build().encode().toUriString();
        try {
            // data.go.kr의 Encoding 키는 이미 퍼센트 인코딩돼 있으므로 그대로 붙인다. (TourApiPlaceProvider와 동일)
            return restClient.get()
                    .uri(URI.create(uri + "&serviceKey=" + properties.getServiceKey()))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException exception) {
            throw TourApiProviderException.fromHttpError(exception);
        } catch (ResourceAccessException exception) {
            throw TourApiProviderException.fromResourceAccessError(exception);
        } catch (RestClientException exception) {
            throw TourApiProviderException.fromClientError(exception);
        }
    }

    private JsonNode firstItem(String response) {
        try {
            JsonNode apiResponse = objectMapper.readTree(response == null ? "" : response).path("response");
            if (!"0000".equals(apiResponse.path("header").path("resultCode").asText())) {
                throw new TourApiProviderException("TourAPI returned an application error.");
            }
            JsonNode items = apiResponse.path("body").path("items").path("item");
            if (items.isArray()) {
                return items.isEmpty() ? null : items.get(0);
            }
            return items.isObject() ? items : null;
        } catch (JsonProcessingException exception) {
            throw new TourApiProviderException("TourAPI returned an invalid JSON response.", exception);
        }
    }
}
