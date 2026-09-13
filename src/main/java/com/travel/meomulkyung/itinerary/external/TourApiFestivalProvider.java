package com.travel.meomulkyung.itinerary.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.meomulkyung.region.domain.Region;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class TourApiFestivalProvider implements FestivalProvider {
    private static final DateTimeFormatter TOUR_API_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String FESTIVAL_CONTENT_TYPE_ID = "15";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final TourApiProperties properties;

    public TourApiFestivalProvider(RestClient restClient, ObjectMapper objectMapper, TourApiProperties properties) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public List<Festival> findFestivals(Region region, LocalDate start, LocalDate end) {
        TourApiProperties.RegionCode regionCode = properties.getRegionCodes().get(region.getId());
        if (regionCode == null || !StringUtils.hasText(regionCode.getAreaCode())) {
            throw new TourApiProviderException("TourAPI region mapping is missing.");
        }
        if (!StringUtils.hasText(properties.getServiceKey())) {
            throw new TourApiProviderException("TourAPI service key is not configured.");
        }

        try {
            String response = restClient.get().uri(requestUri(regionCode, start, end)).retrieve().body(String.class);
            return parseResponse(response);
        } catch (RestClientResponseException exception) {
            throw TourApiProviderException.fromHttpError(exception);
        } catch (ResourceAccessException exception) {
            throw TourApiProviderException.fromResourceAccessError(exception);
        } catch (RestClientException exception) {
            throw TourApiProviderException.fromClientError(exception);
        }
    }

    private URI requestUri(TourApiProperties.RegionCode regionCode, LocalDate start, LocalDate end) {
        String uri = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .pathSegment("searchFestival2")
                .queryParam("MobileOS", properties.getMobileOs())
                .queryParam("MobileApp", properties.getMobileApp())
                .queryParam("_type", "json")
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", properties.getPageSize())
                .queryParam("areaCode", regionCode.getAreaCode())
                .queryParam("contentTypeId", FESTIVAL_CONTENT_TYPE_ID)
                .queryParam("eventStartDate", TOUR_API_DATE.format(start))
                .queryParam("eventEndDate", TOUR_API_DATE.format(end))
                .queryParamIfPresent("sigunguCode", StringUtils.hasText(regionCode.getSigunguCode())
                        ? java.util.Optional.of(regionCode.getSigunguCode()) : java.util.Optional.empty())
                .build()
                .encode()
                .toUriString();

        // data.go.kr's Encoding key is already percent-encoded and must remain unchanged.
        return URI.create(uri + (uri.contains("?") ? "&" : "?") + "serviceKey=" + properties.getServiceKey());
    }

    private List<Festival> parseResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode apiResponse = root.path("response");
            if (!"0000".equals(apiResponse.path("header").path("resultCode").asText())) {
                throw new TourApiProviderException("TourAPI returned an application error.");
            }

            JsonNode items = apiResponse.path("body").path("items").path("item");
            if (items.isMissingNode() || items.isNull()) {
                return List.of();
            }
            List<Festival> festivals = new ArrayList<>();
            if (items.isArray()) {
                items.forEach(item -> addFestival(festivals, item));
            } else if (items.isObject()) {
                addFestival(festivals, items);
            }
            return festivals;
        } catch (JsonProcessingException exception) {
            throw new TourApiProviderException("TourAPI returned an invalid JSON response.", exception);
        }
    }

    private void addFestival(List<Festival> festivals, JsonNode item) {
        Long contentId = longValue(item, "contentid");
        LocalDate startDate = dateValue(item, "eventstartdate");
        LocalDate endDate = dateValue(item, "eventenddate");
        if (contentId == null || startDate == null || endDate == null) {
            return;
        }
        festivals.add(new Festival(
                contentId,
                text(item, "title"),
                startDate,
                endDate,
                firstNonBlank(text(item, "firstimage"), text(item, "firstimage2")),
                address(item)));
    }

    private String address(JsonNode item) {
        String address = text(item, "addr1");
        String detail = text(item, "addr2");
        if (!StringUtils.hasText(address)) {
            return detail;
        }
        return StringUtils.hasText(detail) ? address + " " + detail : address;
    }

    private String text(JsonNode item, String field) {
        String value = item.path(field).asText(null);
        return StringUtils.hasText(value) ? value : null;
    }

    private Long longValue(JsonNode item, String field) {
        JsonNode value = item.path(field);
        if (value.canConvertToLong()) {
            return value.longValue();
        }
        try {
            return StringUtils.hasText(value.asText()) ? Long.valueOf(value.asText()) : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private LocalDate dateValue(JsonNode item, String field) {
        try {
            String value = text(item, field);
            return StringUtils.hasText(value) ? LocalDate.parse(value, TOUR_API_DATE) : null;
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }
}
