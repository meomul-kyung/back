package com.travel.meomulkyung;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:application-context;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=application-context-test-secret-that-is-at-least-32-bytes",
        "jwt.access-token-validity=1800000",
        "spring.security.oauth2.client.registration.kakao.client-id=test-kakao",
        "spring.security.oauth2.client.registration.kakao.client-secret=test-kakao-secret",
        "spring.security.oauth2.client.registration.google.client-id=test-google",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "spring.security.oauth2.client.registration.naver.client-id=test-naver",
        "spring.security.oauth2.client.registration.naver.client-secret=test-naver-secret"
})
class MeomulKyungApplicationTests {

    @Test
    void contextLoads() {
    }

}
