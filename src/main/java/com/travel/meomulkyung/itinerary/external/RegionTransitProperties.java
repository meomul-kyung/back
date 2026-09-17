package com.travel.meomulkyung.itinerary.external;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 지역별 버스 안내 설정.
 *
 * <p>외부 API가 아니라 지자체 공식 안내 페이지 링크와 요금 정책이라 호출 없이 설정값으로만 제공한다.
 * 그래서 TourAPI 무캐싱 규정과 무관하고, 응답 지연도 생기지 않는다.
 *
 * <p>URL은 ASCII만 들어가므로 {@code application.properties}에 두고,
 * 한글 문구(출처 표기·배지 문구)는 properties 파일의 인코딩 문제를 피하려고
 * 이 클래스의 기본값으로 둔다. 필요하면 properties에서 덮어쓸 수 있다.
 *
 * <p>키는 {@code region_id}이며 {@code tour-api.region-codes}, {@code kma.coordinates}와 같은 체계다.
 */
@ConfigurationProperties("region-transit")
public class RegionTransitProperties {

    /** region_id -> 공식 버스 시간표 안내 페이지. 설정에 없으면 시간표를 내리지 않는다. */
    private Map<Long, String> timetableUrls = new LinkedHashMap<>();

    /** region_id -> 출처 표기. 기본값은 운영 지역 15곳의 안내 주체다. */
    private Map<Long, String> timetableSources = defaultSources();

    /** 관내 버스 요금이 무료인 지역의 region_id. */
    private Set<Long> freeBusRegions = new LinkedHashSet<>();

    private String freeBusLabel = "관내 버스 무료";

    /** 지역이 무료여도 시·군을 넘는 노선은 유료인 경우가 있어 배지와 항상 같이 내린다. */
    private String freeBusCaution = "다른 시·군을 오가는 노선은 유료일 수 있어요";

    /** 무료 판단의 근거. 요금 정책은 조례로 바뀔 수 있어 근거를 함께 노출한다. */
    private String freeBusBasis = "2025년 7월 기준 보도";

    /** 링크와 요금 정책을 마지막으로 확인한 날짜. 응답에 그대로 내려 사용자가 판단하게 한다. */
    private LocalDate checkedOn;

    public String timetableUrl(Long regionId) {
        return regionId == null ? null : timetableUrls.get(regionId);
    }

    public String timetableSource(Long regionId) {
        return regionId == null ? null : timetableSources.get(regionId);
    }

    public boolean isFreeBusRegion(Long regionId) {
        return regionId != null && freeBusRegions.contains(regionId);
    }

    private static Map<Long, String> defaultSources() {
        Map<Long, String> sources = new LinkedHashMap<>();
        sources.put(1L, "안동시 시내버스 정보");
        sources.put(2L, "영주시 시내버스 정보");
        sources.put(3L, "문경시 시내버스 정보");
        sources.put(4L, "상주시 시내버스 정보");
        sources.put(5L, "봉화군 대중교통 정보");
        sources.put(6L, "영양군 농어촌버스 정보");
        sources.put(7L, "청송군 농어촌버스 정보");
        sources.put(8L, "의성군 농어촌버스 정보");
        sources.put(9L, "청도군 농어촌버스 정보");
        sources.put(10L, "영천시 버스정보시스템");
        sources.put(11L, "울진군 농어촌버스 정보");
        sources.put(12L, "영덕군 농어촌버스 정보");
        sources.put(13L, "고령군 농어촌버스 정보");
        sources.put(14L, "성주군 농어촌버스 정보");
        sources.put(15L, "울릉군 농어촌버스 정보");
        return sources;
    }

    public Map<Long, String> getTimetableUrls() { return timetableUrls; }
    public void setTimetableUrls(Map<Long, String> timetableUrls) { this.timetableUrls = timetableUrls; }
    public Map<Long, String> getTimetableSources() { return timetableSources; }
    public void setTimetableSources(Map<Long, String> timetableSources) { this.timetableSources = timetableSources; }
    public Set<Long> getFreeBusRegions() { return freeBusRegions; }
    public void setFreeBusRegions(Set<Long> freeBusRegions) { this.freeBusRegions = freeBusRegions; }
    public String getFreeBusLabel() { return freeBusLabel; }
    public void setFreeBusLabel(String freeBusLabel) { this.freeBusLabel = freeBusLabel; }
    public String getFreeBusCaution() { return freeBusCaution; }
    public void setFreeBusCaution(String freeBusCaution) { this.freeBusCaution = freeBusCaution; }
    public String getFreeBusBasis() { return freeBusBasis; }
    public void setFreeBusBasis(String freeBusBasis) { this.freeBusBasis = freeBusBasis; }
    public LocalDate getCheckedOn() { return checkedOn; }
    public void setCheckedOn(LocalDate checkedOn) { this.checkedOn = checkedOn; }
}
