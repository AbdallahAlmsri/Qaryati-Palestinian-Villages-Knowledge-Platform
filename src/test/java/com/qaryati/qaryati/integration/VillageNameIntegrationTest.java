package com.qaryati.qaryati.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VillageNameIntegrationTest extends AbstractIntegrationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String VILLAGE_JSON =
            "{\"governorateId\":1,\"latitude\":32.25,\"longitude\":35.3,"
                    + "\"locationDescription\":\"Village name integration test\",\"elevationM\":600}";

    @Test
    void getNames_isPublic() throws Exception {
        ApiResponse response = call(
                "GET", "/api/v1/villages/1/names", null, null
        );

        assertEquals(200, response.status());
    }

    @Test
    void createName_withoutToken_isRejected() throws Exception {
        String json = "{\"name\":\"Test Village\",\"nameType\":\"ALTERNATIVE\"}";

        ApiResponse response = call(
                "POST", "/api/v1/villages/1/names", null, json
        );

        assertEquals(401, response.status());
    }

    @Test
    void createName_withToken_returns201AndPending() throws Exception {
        String token = registerUser(uniqueName("villageName"));

        String json = "{\"name\":\"Test Village Name\","
                + "\"nameType\":\"ALTERNATIVE\","
                + "\"language\":\"ar\","
                + "\"nameOrigin\":\"Historical name origin\"}";

        ApiResponse response = call(
                "POST", "/api/v1/villages/1/names", token, json
        );

        assertEquals(201, response.status());
        assertTrue(response.body().contains("Test Village Name"));
        assertTrue(response.body().contains("PENDING"));
        assertTrue(response.body().contains("Historical name origin"));
    }

    @Test
    void createName_withBlankName_returnsValidationError() throws Exception {
        String token = registerUser(uniqueName("villageName"));

        String json = "{\"name\":\"\",\"nameType\":\"ALTERNATIVE\"}";

        ApiResponse response = call(
                "POST", "/api/v1/villages/1/names", token, json
        );

        assertEquals(422, response.status());
        assertTrue(response.body().contains("VALIDATION_FAILED"));
    }

    @Test
    void searchVillage_byAlternativeName_returnsVillage() throws Exception {
        String token = registerUser(uniqueName("villageName"));

        ApiResponse villageResponse = call(
                "POST", "/api/v1/villages", token, VILLAGE_JSON
        );

        assertEquals(201, villageResponse.status());

        JsonNode village = MAPPER.readTree(villageResponse.body());
        long villageId = village.get("id").asLong();

        String marker = uniqueName("AlternativeName");

        String json = "{\"name\":\"" + marker + "\","
                + "\"nameType\":\"ALTERNATIVE\"}";

        ApiResponse nameResponse = call(
                "POST",
                "/api/v1/villages/" + villageId + "/names",
                token,
                json
        );

        assertEquals(201, nameResponse.status());

        ApiResponse searchResponse = call(
                "GET",
                "/api/v1/villages?q=" + marker,
                null,
                null
        );

        assertEquals(200, searchResponse.status());
        assertTrue(searchResponse.body().contains("\"totalItems\":1"));
        assertTrue(searchResponse.body().contains("\"id\":" + villageId));
    }
}