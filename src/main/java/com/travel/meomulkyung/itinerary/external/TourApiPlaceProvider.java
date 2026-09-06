package com.travel.meomulkyung.itinerary.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.meomulkyung.itinerary.domain.ItineraryItemType;
import com.travel.meomulkyung.region.domain.Region;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TourApiPlaceProvider implements TourPlaceProvider {
    private static final Set<String> ALLOWED_CONTENT_TYPE_IDS = Set.of("12", "14", "28", "38", "39");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final TourApiProperties properties;

    public TourApiPlaceProvider(RestClient restClient, ObjectMapper objectMapper, TourApiProperties properties) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public List<Place> findPlaces(Region region) {
        TourApiProperties.RegionCode regionCode = properties.getRegionCodes().get(region.getId());
        if (regionCode == null || !StringUtils.hasText(regionCode.getAreaCode())) {
            throw new TourApiProviderException("TourAPI region mapping is missing.");
        }
        if (!StringUtils.hasText(properties.getServiceKey())) {
            throw new TourApiProviderException("TourAPI service key is not configured.");
        }

        try {
            String response = restClient.get().uri(requestUri(regionCode)).retrieve().body(String.class);
            return parseResponse(response);
        } catch (RestClientResponseException exception) {
            throw new TourApiProviderException("TourAPI returned an HTTP error.");
        } catch (ResourceAccessException exception) {
            throw new TourApiProviderException("TourAPI request failed.");
        } catch (RestClientException exception) {
            throw new TourApiProviderException("TourAPI request failed.");
        }
    }

    private URI requestUri(TourApiProperties.RegionCode regionCode) {
        String uri = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .pathSegment("areaBasedList2")
                .queryParam("MobileOS", properties.getMobileOs())
                .queryParam("MobileApp", properties.getMobileApp())
                .queryParam("_type", "json")
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", properties.getPageSize())
                .queryParam("areaCode", regionCode.getAreaCode())
                .queryParamIfPresent("sigunguCode", StringUtils.hasText(regionCode.getSigunguCode())
                        ? java.util.Optional.of(regionCode.getSigunguCode()) : java.util.Optional.empty())
                .build()
                .encode()
                .toUriString();

        // data.go.kr's Encoding key is already percent-encoded and must remain unchanged.
        return URI.create(uri + (uri.contains("?") ? "&" : "?") + "serviceKey=" + properties.getServiceKey());
    }

    private List<Place> parseResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode apiResponse = root.path("response");
            JsonNode header = apiResponse.path("header");
            String resultCode = header.path("resultCode").asText();
            if (!"0000".equals(resultCode)) {
                throw new TourApiProviderException("TourAPI returned an application error.");
            }

            JsonNode items = apiResponse.path("body").path("items").path("item");
            if (items.isMissingNode() || items.isNull()) {
                return List.of();
            }
            List<Place> places = new ArrayList<>();
            if (items.isArray()) {
                items.forEach(item -> addPlace(places, item));
            } else if (items.isObject()) {
                addPlace(places, items);
            }
            return places;
        } catch (JsonProcessingException exception) {
            throw new TourApiProviderException("TourAPI returned an invalid JSON response.");
        }
    }

    private void addPlace(List<Place> places, JsonNode item) {
        Long contentId = longValue(item, "contentid");
        String contentTypeId = text(item, "contenttypeid");
        if (contentId == null || !StringUtils.hasText(contentTypeId) || !ALLOWED_CONTENT_TYPE_IDS.contains(contentTypeId)) {
            return;
        }
        places.add(new Place(
                contentId,
                text(item, "title"),
                itemType(contentTypeId),
                firstNonBlank(text(item, "firstimage"), text(item, "firstimage2")),
                address(item),
                doubleValue(item, "mapy"),
                doubleValue(item, "mapx")));
    }

    private ItineraryItemType itemType(String contentTypeId) {
        if ("39".equals(contentTypeId)) {
            return ItineraryItemType.RESTAURANT;
        }
        if ("28".equals(contentTypeId)) {
            return ItineraryItemType.EXPERIENCE;
        }
        return ItineraryItemType.TOURIST_SPOT;
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

    private Double doubleValue(JsonNode item, String field) {
        JsonNode value = item.path(field);
        return value.isNumber() || value.isTextual() && StringUtils.hasText(value.asText())
                ? value.asDouble() : null;
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }
}
