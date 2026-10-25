package com.qaryati.qaryati.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HistoryWorkflowIntegrationTest extends AbstractIntegrationTest {

    private static final String VILLAGE_JSON =
            "{\"governorateId\":1,\"latitude\":32.25,\"longitude\":35.3,"
                    + "\"locationDescription\":\"History test village\",\"elevationM\":600}";

    @Test
    void historicalEventReviewWorkflow() throws Exception {
        String contributor = registerUser(uniqueName("histcontrib"));
        String verifier = registerVerifier(uniqueName("histverif"));

        ApiResponse village = call("POST", "/api/v1/villages", contributor, VILLAGE_JSON);
        assertEquals(201, village.status());

        long villageId = extractLong(village.body(), "id");
        String base = "/api/v1/villages/" + villageId + "/historical-events";

        String eventJson = "{\"title\":\"Historical Event\","
                + "\"description\":\"An important event in village history\","
                + "\"eventDate\":\"1948-05-15\","
                + "\"source\":\"Historical archive\"}";

        ApiResponse created = call("POST", base, contributor, eventJson);

        assertEquals(201, created.status());
        assertTrue(created.body().contains("\"status\":\"PENDING\""));

        long eventId = extractLong(created.body(), "id");

        ApiResponse history = call("GET", base, null, null);
        assertEquals(200, history.status());
        assertTrue(history.body().contains("Historical Event"));

        String reviewPath = base + "/" + eventId + "/review";
        String approve = "{\"status\":\"VERIFIED\",\"reviewNotes\":\"checked\"}";

        assertEquals(403, call("PATCH", reviewPath, contributor, approve).status());

        ApiResponse reviewed = call("PATCH", reviewPath, verifier, approve);
        assertEquals(200, reviewed.status());
        assertTrue(reviewed.body().contains("\"status\":\"VERIFIED\""));

        ApiResponse again = call("PATCH", reviewPath, verifier, approve);
        assertEquals(422, again.status());
        assertTrue(again.body().contains("INVALID_HISTORY_REVIEW"));
    }

    @Test
    void landmarkReviewWorkflow() throws Exception {
        String contributor = registerUser(uniqueName("landcontrib"));
        String verifier = registerVerifier(uniqueName("landverif"));

        ApiResponse village = call("POST", "/api/v1/villages", contributor, VILLAGE_JSON);
        assertEquals(201, village.status());

        long villageId = extractLong(village.body(), "id");
        String base = "/api/v1/villages/" + villageId + "/landmarks";

        String landmarkJson = "{\"name\":\"Old Mosque\","
                + "\"description\":\"Historical landmark\","
                + "\"landmarkType\":\"MOSQUE\","
                + "\"latitude\":32.25,"
                + "\"longitude\":35.3,"
                + "\"source\":\"Village archive\"}";

        ApiResponse created = call("POST", base, contributor, landmarkJson);
        assertEquals(201, created.status());
        assertTrue(created.body().contains("\"status\":\"PENDING\""));

        long landmarkId = extractLong(created.body(), "id");

        ApiResponse landmarks = call("GET", base, null, null);
        assertEquals(200, landmarks.status());
        assertTrue(landmarks.body().contains("Old Mosque"));

        String reviewPath = base + "/" + landmarkId + "/review";
        String approve = "{\"status\":\"VERIFIED\"}";

        assertEquals(403, call("PATCH", reviewPath, contributor, approve).status());

        ApiResponse reviewed = call("PATCH", reviewPath, verifier, approve);
        assertEquals(200, reviewed.status());
        assertTrue(reviewed.body().contains("\"status\":\"VERIFIED\""));

        ApiResponse again = call("PATCH", reviewPath, verifier, approve);
        assertEquals(422, again.status());
        assertTrue(again.body().contains("INVALID_HISTORY_REVIEW"));
    }

    @Test
    void villageAttributeReviewWorkflow() throws Exception {
        String contributor = registerUser(uniqueName("attrcontrib"));
        String verifier = registerVerifier(uniqueName("attrverif"));

        ApiResponse village = call("POST", "/api/v1/villages", contributor, VILLAGE_JSON);
        assertEquals(201, village.status());

        long villageId = extractLong(village.body(), "id");
        String base = "/api/v1/villages/" + villageId + "/attributes";

        String attributeJson = "{\"attributeName\":\"Traditional Food\","
                + "\"attributeValue\":\"Musakhan\","
                + "\"source\":\"Village heritage archive\"}";

        ApiResponse created = call("POST", base, contributor, attributeJson);
        assertEquals(201, created.status());
        assertTrue(created.body().contains("\"status\":\"PENDING\""));

        long attributeId = extractLong(created.body(), "id");

        ApiResponse attributes = call("GET", base, null, null);
        assertEquals(200, attributes.status());
        assertTrue(attributes.body().contains("Traditional Food"));

        String reviewPath = base + "/" + attributeId + "/review";
        String reject = "{\"status\":\"REJECTED\",\"reviewNotes\":\"Unverified source\"}";

        assertEquals(403, call("PATCH", reviewPath, contributor, reject).status());

        ApiResponse reviewed = call("PATCH", reviewPath, verifier, reject);
        assertEquals(200, reviewed.status());
        assertTrue(reviewed.body().contains("\"status\":\"REJECTED\""));

        ApiResponse again = call("PATCH", reviewPath, verifier, reject);
        assertEquals(422, again.status());
        assertTrue(again.body().contains("INVALID_HISTORY_REVIEW"));
    }

    @Test
    void historyValidationAndErrorHandling() throws Exception {
        String contributor = registerUser(uniqueName("histerrors"));

        ApiResponse village = call("POST", "/api/v1/villages", contributor, VILLAGE_JSON);
        assertEquals(201, village.status());

        long villageId = extractLong(village.body(), "id");
        String base = "/api/v1/villages/" + villageId;

        ApiResponse missingVillage = call(
                "GET", "/api/v1/villages/999999/historical-events", null, null);
        assertEquals(422, missingVillage.status());
        assertTrue(missingVillage.body().contains("VILLAGE_NOT_FOUND"));

        ApiResponse invalidEvent = call(
                "POST", base + "/historical-events", contributor,
                "{\"title\":\"\",\"description\":\"Test\",\"source\":\"Archive\"}");
        assertEquals(422, invalidEvent.status());
        assertTrue(invalidEvent.body().contains("VALIDATION_FAILED"));

        ApiResponse invalidLandmark = call(
                "POST", base + "/landmarks", contributor,
                "{\"name\":\"Old Building\",\"latitude\":95,\"source\":\"Archive\"}");
        assertEquals(422, invalidLandmark.status());
        assertTrue(invalidLandmark.body().contains("VALIDATION_FAILED"));

        ApiResponse invalidAttribute = call(
                "POST", base + "/attributes", contributor,
                "{\"attributeName\":\"\",\"attributeValue\":\"Value\",\"source\":\"Archive\"}");
        assertEquals(422, invalidAttribute.status());
        assertTrue(invalidAttribute.body().contains("VALIDATION_FAILED"));

        ApiResponse unauthenticated = call(
                "POST", base + "/historical-events", null,
                "{\"title\":\"Event\",\"description\":\"Description\",\"source\":\"Archive\"}");
        assertEquals(401, unauthenticated.status());
    }
}