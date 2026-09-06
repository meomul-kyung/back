package com.travel.meomulkyung.itinerary.external;

/**
 * 위경도 → 기상청 동네예보 격자(nx, ny) 변환.
 *
 * <p>기상청이 공개한 Lambert Conformal Conic 투영 파라미터를 그대로 사용한다.
 * 단기예보 API는 위경도가 아니라 5km 격자 번호를 요구하므로, 지역별 위경도만 설정에 두고
 * 격자는 여기서 계산한다. (격자표를 직접 옮겨 적는 것보다 검증이 쉽다.)
 */
public final class KmaGridConverter {

    private static final double EARTH_RADIUS_KM = 6371.00877;
    private static final double GRID_KM = 5.0;
    private static final double STANDARD_LATITUDE_1 = 30.0;
    private static final double STANDARD_LATITUDE_2 = 60.0;
    private static final double ORIGIN_LONGITUDE = 126.0;
    private static final double ORIGIN_LATITUDE = 38.0;
    private static final double ORIGIN_X = 43.0;
    private static final double ORIGIN_Y = 136.0;

    private static final double DEGREE_TO_RADIAN = Math.PI / 180.0;

    private KmaGridConverter() {
    }

    public record Grid(int nx, int ny) {
    }

    public static Grid toGrid(double latitude, double longitude) {
        double re = EARTH_RADIUS_KM / GRID_KM;
        double slat1 = STANDARD_LATITUDE_1 * DEGREE_TO_RADIAN;
        double slat2 = STANDARD_LATITUDE_2 * DEGREE_TO_RADIAN;
        double olon = ORIGIN_LONGITUDE * DEGREE_TO_RADIAN;
        double olat = ORIGIN_LATITUDE * DEGREE_TO_RADIAN;

        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);

        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;

        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);

        double ra = Math.tan(Math.PI * 0.25 + latitude * DEGREE_TO_RADIAN * 0.5);
        ra = re * sf / Math.pow(ra, sn);

        double theta = longitude * DEGREE_TO_RADIAN - olon;
        if (theta > Math.PI) {
            theta -= 2.0 * Math.PI;
        }
        if (theta < -Math.PI) {
            theta += 2.0 * Math.PI;
        }
        theta *= sn;

        int nx = (int) Math.floor(ra * Math.sin(theta) + ORIGIN_X + 0.5);
        int ny = (int) Math.floor(ro - ra * Math.cos(theta) + ORIGIN_Y + 0.5);
        return new Grid(nx, ny);
    }
}
