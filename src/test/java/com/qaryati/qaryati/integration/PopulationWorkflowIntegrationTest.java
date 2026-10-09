package com.qaryati.qaryati.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PopulationWorkflowIntegrationTest extends AbstractIntegrationTest {

    private static final String VILLAGE_JSON =
            "{\"governorateId\":1,\"latitude\":32.25,\"longitude\":35.3,"
                    + "\"locationDescription\":\"Workflow test village\",\"elevationM\":600}";

    private String record(int year, int population, String source) {
        return "{\"population\":%d,\"year\":%d,\"source\":\"%s\"}".formatted(population, year, source);
    }

    @Test
    void verificationWorkflow_onlyVerifiedRecordsCountInStats() throws Exception {
        String contributor = registerUser(uniqueName("contrib"));
        String verifier = registerVerifier(uniqueName("verif"));

        long villageId = extractLong(call("POST", "/api/v1/villages", contributor, VILLAGE_JSON).body(), "id");
        String base = "/api/v1/villages/" + villageId + "/population";

        ApiResponse first = call("POST", base, contributor, record(2015, 2000, "PCBS 2015"));
        assertEquals(201, first.status());
        assertTrue(first.body().contains("\"status\":\"PENDING\""));
        long recordId = extractLong(first.body(), "id");

        assertEquals(201, call("POST", base, contributor, record(2016, 9999, "Unverified source")).status());

        String reviewPath = base + "/" + recordId + "/review";
        String approve = "{\"status\":\"VERIFIED\",\"reviewNotes\":\"checked\"}";

        assertEquals(403, call("PATCH", reviewPath, contributor, approve).status());

        ApiResponse reviewed = call("PATCH", reviewPath, verifier, approve);
        assertEquals(200, reviewed.status());
        assertTrue(reviewed.body().contains("\"status\":\"VERIFIED\""));

        ApiResponse again = call("PATCH", reviewPath, verifier, approve);
        assertEquals(422, again.status());
        assertTrue(again.body().contains("INVALID_REVIEW"));

        ApiResponse stats = call("GET", base + "/stats?fromYear=2000&toYear=2025", null, null);
        assertEquals(200, stats.status());
        assertTrue(stats.body().contains("\"recordCount\":1"));
        assertTrue(stats.body().contains("\"maxPopulation\":2000"));
    }

    @Test
    void duplicateRecord_returns409() throws Exception {
        String contributor = registerUser(uniqueName("contrib"));
        long villageId = extractLong(call("POST", "/api/v1/villages", contributor, VILLAGE_JSON).body(), "id");
        String base = "/api/v1/villages/" + villageId + "/population";

        assertEquals(201, call("POST", base, contributor, record(2018, 100, "Same source")).status());

        ApiResponse duplicate = call("POST", base, contributor, record(2018, 100, "Same source"));

        assertEquals(409, duplicate.status());
        assertTrue(duplicate.body().contains("DUPLICATE_OR_INVALID_DATA"));
    }

    @Test
    void populationHistory_forUnknownVillage_returns422() throws Exception {
        ApiResponse response = call("GET", "/api/v1/villages/999999/population", null, null);

        assertEquals(422, response.status());
        assertTrue(response.body().contains("VILLAGE_NOT_FOUND"));
    }
}