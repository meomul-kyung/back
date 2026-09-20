package com.travel.meomulkyung.recommendation;

import com.travel.meomulkyung.recommendation.external.TourDemandProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Map;

@TestConfiguration
public class RecommendationTestConfig {

    @Bean
    @Primary
    FakeTourDemandProvider fakeTourDemandProvider() {
        return new FakeTourDemandProvider();
    }

    /**
     * 기본값은 빈 Map이다. 수요 강도를 쓰지 않는 테스트는 기존 하드코딩 범위 방식 그대로 돌아간다.
     */
    public static class FakeTourDemandProvider implements TourDemandProvider {

        private Map<Long, StayProfile> profiles = Map.of();

        public void setProfiles(Map<Long, StayProfile> profiles) {
            this.profiles = profiles;
        }

        public void reset() {
            profiles = Map.of();
        }

        @Override
        public Map<Long, StayProfile> findStayProfiles() {
            return profiles;
        }
    }
}
