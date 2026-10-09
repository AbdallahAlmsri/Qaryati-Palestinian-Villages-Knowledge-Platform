package com.qaryati.qaryati.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VillageApiIntegrationTest extends AbstractIntegrationTest {

    private static final String VALID_VILLAGE =
            "{\"governorateId\":1,\"latitude\":32.25,\"longitude\":35.3,"
                    + "\"locationDescription\":\"Integration test village\",\"elevationM\":600}";

    @Test
    void listVillages_isPublic_andIncludesSeedData() throws Exception {
        ApiResponse response = call("GET", "/api/v1/villages", null, null);

        assertEquals(200, response.status());
        assertTrue(response.body().contains("Nablus"));
    }

    @Test
    void createVillage_withoutToken_isRejected() throws Exception {
        ApiResponse response = call("POST", "/api/v1/villages", null, VALID_VILLAGE);

        assertEquals(401, response.status());
    }

    @Test
    void createVillage_withToken_returns201() throws Exception {
        String token = registerUser(uniqueName("village"));

        ApiResponse response = call("POST", "/api/v1/villages", token, VALID_VILLAGE);

        assertEquals(201, response.status());
        assertTrue(response.body().contains("Integration test village"));
    }

    @Test
    void createVillage_withUnknownGovernorate_returns422() throws Exception {
        String token = registerUser(uniqueName("village"));
        String json = VALID_VILLAGE.replace("\"governorateId\":1", "\"governorateId\":999");

        ApiResponse response = call("POST", "/api/v1/villages", token, json);

        assertEquals(422, response.status());
        assertTrue(response.body().contains("GOVERNORATE_NOT_FOUND"));
    }

    @Test
    void createVillage_withInvalidLatitude_returnsValidationError() throws Exception {
        String token = registerUser(uniqueName("village"));
        String json = VALID_VILLAGE.replace("\"latitude\":32.25", "\"latitude\":200");

        ApiResponse response = call("POST", "/api/v1/villages", token, json);

        assertEquals(422, response.status());
        assertTrue(response.body().contains("VALIDATION_FAILED"));
    }

    @Test
    void listVillages_searchIsCaseInsensitiveAndEscapesWildcards() throws Exception {
        String token = registerUser(uniqueName("village"));
        String marker = uniqueName("Search");
        String json = VALID_VILLAGE.replace("Integration test village", marker);
        assertEquals(201, call("POST", "/api/v1/villages", token, json).status());

        ApiResponse found = call("GET", "/api/v1/villages?q=" + marker.toUpperCase(), null, null);
        assertEquals(200, found.status());
        assertTrue(found.body().contains("\"totalItems\":1"));

        ApiResponse wildcard = call("GET", "/api/v1/villages?q=%25", null, null);
        assertEquals(200, wildcard.status());
        assertTrue(wildcard.body().contains("\"totalItems\":0"));
    }

    @Test
    void listVillages_respectsPageSizeAndFilter() throws Exception {
        ApiResponse paged = call("GET", "/api/v1/villages?size=1&page=0&sort=id,desc", null, null);
        assertEquals(200, paged.status());
        assertTrue(paged.body().contains("\"size\":1"));

        ApiResponse noMatch = call("GET", "/api/v1/villages?governorateId=999", null, null);
        assertEquals(200, noMatch.status());
        assertTrue(noMatch.body().contains("\"totalItems\":0"));
    }

    @Test
    void listVillages_rejectsInvalidQueries() throws Exception {
        ApiResponse badSort = call("GET", "/api/v1/villages?sort=password,asc", null, null);
        assertEquals(422, badSort.status());
        assertTrue(badSort.body().contains("INVALID_QUERY"));

        ApiResponse tooBig = call("GET", "/api/v1/villages?size=500", null, null);
        assertEquals(422, tooBig.status());
    }
}