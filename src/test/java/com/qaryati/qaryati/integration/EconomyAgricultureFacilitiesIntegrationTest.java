package com.qaryati.qaryati.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EconomyAgricultureFacilitiesIntegrationTest extends AbstractIntegrationTest {

    private static final String VILLAGE_JSON = """
            {
              "governorateId": 1,
              "latitude": 32.25,
              "longitude": 35.3,
              "locationDescription": "T3 test village",
              "elevationM": 600
            }
            """;

    @Test
    void agricultureWorkflow() throws Exception {
        checkWorkflow(
                "agriculture",
                """
                {
                  "cropName": "Olives",
                  "description": "Olive farming",
                  "source": "Agriculture archive"
                }
                """,
                "Olives"
        );
    }

    @Test
    void economyWorkflow() throws Exception {
        checkWorkflow(
                "economy",
                """
                {
                  "activityName": "Handicrafts",
                  "description": "Local crafts",
                  "source": "Economy archive"
                }
                """,
                "Handicrafts"
        );
    }

    @Test
    void facilitiesWorkflow() throws Exception {
        checkWorkflow(
                "facilities",
                """
                {
                  "facilityName": "Village School",
                  "facilityType": "SCHOOL",
                  "description": "Primary school",
                  "source": "Village records"
                }
                """,
                "Village School"
        );
    }

    private void checkWorkflow(
            String endpoint,
            String json,
            String expectedName
    ) throws Exception {

        String contributor = registerUser(uniqueName("t3contrib"));
        String verifier = registerVerifier(uniqueName("t3verif"));

        ApiResponse village = call(
                "POST", "/api/v1/villages", contributor, VILLAGE_JSON);
        assertEquals(201, village.status());

        long villageId = extractLong(village.body(), "id");
        String base = "/api/v1/villages/" + villageId + "/" + endpoint;

        assertEquals(401, call("POST", base, null, json).status());

        ApiResponse invalid = call("POST", base, contributor, "{}");
        assertEquals(422, invalid.status());
        assertTrue(invalid.body().contains("VALIDATION_FAILED"));

        ApiResponse created = call("POST", base, contributor, json);
        assertEquals(201, created.status());
        assertTrue(created.body().contains("PENDING"));

        long recordId = extractLong(created.body(), "id");

        ApiResponse records = call("GET", base, null, null);
        assertEquals(200, records.status());
        assertTrue(records.body().contains(expectedName));

        String reviewPath = base + "/" + recordId + "/review";

        String approve = """
                {"status":"VERIFIED"}
                """;

        assertEquals(403,
                call("PATCH", reviewPath, contributor, approve).status());

        ApiResponse reviewed = call(
                "PATCH", reviewPath, verifier, approve);
        assertEquals(200, reviewed.status());
        assertTrue(reviewed.body().contains("VERIFIED"));

        ApiResponse again = call(
                "PATCH", reviewPath, verifier, approve);
        assertEquals(422, again.status());
        assertTrue(again.body().contains("INVALID_HISTORY_REVIEW"));
    }
}