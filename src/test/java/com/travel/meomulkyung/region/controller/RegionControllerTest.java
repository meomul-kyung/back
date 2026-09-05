package com.travel.meomulkyung.region.controller;

import com.travel.meomulkyung.region.RegionTestFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:region-api;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "jwt.secret=region-api-test-secret-that-is-at-least-32-bytes",
        "jwt.access-token-validity=1800000",
        "spring.security.oauth2.client.registration.kakao.client-id=test-kakao",
        "spring.security.oauth2.client.registration.kakao.client-secret=test-kakao-secret",
        "spring.security.oauth2.client.registration.google.client-id=test-google",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "spring.security.oauth2.client.registration.naver.client-id=test-naver",
        "spring.security.oauth2.client.registration.naver.client-secret=test-naver-secret"
})
@AutoConfigureMockMvc
class RegionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getRegionsWithoutAuthenticationReturnsOk() throws Exception {
        mockMvc.perform(get("/api/regions"))
                .andExpect(status().isOk());
    }

    @Test
    void getRegionsReturnsAllFifteenInAscendingIdOrder() throws Exception {
        mockMvc.perform(get("/api/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(15))
                .andExpect(jsonPath("$[*].regionId", contains(RegionTestFixture.REGION_IDS.toArray())));
    }

    @Test
    void getRegionsReturnsOnlyListFieldsAndRepresentativeTags() throws Exception {
        mockMvc.perform(get("/api/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].regionId").value(RegionTestFixture.FIRST_REGION_ID))
                .andExpect(jsonPath("$[0].regionName").value(RegionTestFixture.FIRST_REGION_NAME))
                .andExpect(jsonPath("$[0].thumbnailUrl").value(nullValue()))
                .andExpect(jsonPath("$[0].identityStatement").isNotEmpty())
                .andExpect(jsonPath("$[0].representativeTags[0].code").value("HISTORY"))
                .andExpect(jsonPath("$[0].representativeTags[0].label").isNotEmpty())
                .andExpect(jsonPath("$[0].representativeResources").doesNotExist());
    }

    @Test
    void getRegionWithoutAuthenticationReturnsOk() throws Exception {
        mockMvc.perform(get("/api/regions/{regionId}", RegionTestFixture.FIRST_REGION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regionId").value(RegionTestFixture.FIRST_REGION_ID))
                .andExpect(jsonPath("$.regionName").value(RegionTestFixture.FIRST_REGION_NAME));
    }

    @Test
    void getRegionReturnsResourcesTravelStyleTipsAndAttributions() throws Exception {
        mockMvc.perform(get("/api/regions/{regionId}", RegionTestFixture.FIRST_REGION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.representativeResources[0].placeName").value(RegionTestFixture.FIRST_REPRESENTATIVE_PLACE))
                .andExpect(jsonPath("$.representativeResources[0].placeId").value(nullValue()))
                .andExpect(jsonPath("$.representativeResources[0].category").value(nullValue()))
                .andExpect(jsonPath("$.travelStyle.keywords[0].code").value("HISTORY"))
                .andExpect(jsonPath("$.travelStyle.recommendedCompanions[0].code").value("FRIENDS"))
                .andExpect(jsonPath("$.localTips.length()").value(0))
                .andExpect(jsonPath("$.sourceAttributions.length()").value(0))
                .andExpect(jsonPath("$.lastUpdatedAt").isNotEmpty());
    }

    @Test
    void getUnknownRegionReturnsRegionNotFound() throws Exception {
        mockMvc.perform(get("/api/regions/{regionId}", 9999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REGION_NOT_FOUND"));
    }

    @Test
    void writeMethodsAreNotPublic() throws Exception {
        mockMvc.perform(post("/api/regions"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(put("/api/regions/{regionId}", RegionTestFixture.FIRST_REGION_ID))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(delete("/api/regions/{regionId}", RegionTestFixture.FIRST_REGION_ID))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void regionResponsesDoNotExposeSecretsOrAdministrationFields() throws Exception {
        mockMvc.perform(get("/api/regions/{regionId}", RegionTestFixture.FIRST_REGION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiKey").doesNotExist())
                .andExpect(jsonPath("$.jwtSecret").doesNotExist())
                .andExpect(jsonPath("$.oauthClientSecret").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist())
                .andExpect(jsonPath("$.updatedAt").doesNotExist())
                .andExpect(jsonPath("$.representativeResources[0].placeName", not("")));
    }
}
