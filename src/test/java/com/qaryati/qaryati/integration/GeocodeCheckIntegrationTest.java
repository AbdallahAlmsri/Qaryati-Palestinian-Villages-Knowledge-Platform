package com.qaryati.qaryati.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GET /villages/{id}/geocode-check only looks at VERIFIED Arabic names. The unit tests mock the repository, so none of
 * them ran that query against a real database. The query once compared the enum column "status" with a String and
 * every request ended in a 500. This test sends real requests through the whole stack.
 *
 * In the test profile the geocoding service address points at a closed port, so after the name checks pass the
 * provider is unreachable and the endpoint answers 503. That is enough to prove the query ran.
 */
class GeocodeCheckIntegrationTest extends AbstractIntegrationTest {

    @Test
    void geocodeCheck_usesOnlyAVerifiedArabicName() throws Exception {
        String contributor = registerUser(uniqueName("geocontrib"));
        String verifierName = uniqueName("geoverifier");
        registerUser(verifierName);
        jdbcTemplate.update("UPDATE users SET role = 'VERIFIER' WHERE username = ?", verifierName);
        String verifier = login(verifierName);

        ApiResponse village = call("POST", "/api/v1/villages", contributor,
                "{\"governorateId\":1,\"latitude\":32.25,\"longitude\":35.3,"
                        + "\"locationDescription\":\"Geocode check\",\"elevationM\":600}");
        assertEquals(201, village.status(), village.body());
        long villageId = extractLong(village.body(), "id");
        String checkPath = "/api/v1/villages/" + villageId + "/geocode-check";

        ApiResponse noName = call("GET", checkPath, null, null);
        assertEquals(422, noName.status(), noName.body());
        assertTrue(noName.body().contains("MISSING_VERIFIED_ARABIC_NAME"), noName.body());

        ApiResponse added = call("POST", "/api/v1/villages/" + villageId + "/names", contributor,
                "{\"name\":\"Huwara\",\"nameType\":\"current\",\"language\":\"ar\",\"source\":\"Test\"}");
        assertEquals(201, added.status(), added.body());
        long nameId = extractLong(added.body(), "id");

        ApiResponse stillPending = call("GET", checkPath, null, null);
        assertEquals(422, stillPending.status(), "a PENDING name must not be used: " + stillPending.body());

        ApiResponse reviewed = call("PATCH", "/api/v1/villages/" + villageId + "/names/" + nameId + "/review",
                verifier, "{\"status\":\"VERIFIED\",\"reviewNotes\":\"ok\"}");
        assertEquals(200, reviewed.status(), reviewed.body());

        ApiResponse unreachable = call("GET", checkPath, null, null);
        assertEquals(503, unreachable.status(), unreachable.body());
        assertTrue(unreachable.body().contains("GEOCODING_UNAVAILABLE"), unreachable.body());
    }
}
