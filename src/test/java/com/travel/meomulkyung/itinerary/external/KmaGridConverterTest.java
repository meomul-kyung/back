package com.travel.meomulkyung.itinerary.external;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class KmaGridConverterTest {

    @Test
    @DisplayName("서울시청 좌표는 기상청 예제 격자 (60, 127)로 변환된다")
    void seoulMatchesKnownGrid() {
        KmaGridConverter.Grid grid = KmaGridConverter.toGrid(37.5665, 126.9780);

        assertThat(grid.nx()).isEqualTo(60);
        assertThat(grid.ny()).isEqualTo(127);
    }

    @DisplayName("경북 15개 지역이 서로 다른 격자로 흩어진다")
    @ParameterizedTest(name = "{2}")
    @CsvSource({
            "36.5684, 128.7294, 안동",
            "36.8057, 128.6240, 영주",
            "36.5866, 128.1867, 문경",
            "36.4109, 128.1590, 상주",
            "36.8932, 128.7325, 봉화",
            "36.6666, 129.1124, 영양",
            "36.4364, 129.0570, 청송",
            "36.3527, 128.6970, 의성",
            "35.6476, 128.7341, 청도",
            "36.6577, 128.4527, 예천",
            "36.9930, 129.4005, 울진",
            "36.4152, 129.3656, 영덕",
            "35.7261, 128.2628, 고령",
            "35.9192, 128.2830, 성주",
            "37.4845, 130.9057, 울릉"
    })
    void gyeongbukRegionsProduceValidGrid(double latitude, double longitude, String name) {
        KmaGridConverter.Grid grid = KmaGridConverter.toGrid(latitude, longitude);

        // 기상청 격자는 남한 기준 대략 nx 1~149, ny 1~253 범위 안에 들어온다
        assertThat(grid.nx()).as(name + " nx").isBetween(1, 149);
        assertThat(grid.ny()).as(name + " ny").isBetween(1, 253);
    }

    @Test
    @DisplayName("가까운 지역은 격자도 가깝고, 먼 지역은 격자도 멀다")
    void nearbyRegionsShareNeighbouringGrid() {
        KmaGridConverter.Grid andong = KmaGridConverter.toGrid(36.5684, 128.7294);
        KmaGridConverter.Grid yeongju = KmaGridConverter.toGrid(36.8057, 128.6240);
        KmaGridConverter.Grid ulleung = KmaGridConverter.toGrid(37.4845, 130.9057);

        // 안동-영주는 약 30km 거리 → 격자 5km 기준 10칸 이내
        assertThat(Math.abs(andong.nx() - yeongju.nx())).isLessThanOrEqualTo(10);
        assertThat(Math.abs(andong.ny() - yeongju.ny())).isLessThanOrEqualTo(10);
        // 울릉도는 동해 한가운데라 확연히 떨어져 있어야 한다
        assertThat(Math.abs(andong.nx() - ulleung.nx())).isGreaterThan(20);
    }
}
