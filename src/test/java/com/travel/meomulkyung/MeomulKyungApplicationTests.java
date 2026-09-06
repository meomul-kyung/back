package com.travel.meomulkyung;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:context-load;MODE=MYSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=test-jwt-secret-for-context-loading-only-123456",
        "jwt.access-token-validity=3600000"
})
class MeomulKyungApplicationTests {

    @Test
    void contextLoads() {
    }

}
