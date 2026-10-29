package com.qaryati.qaryati.integration;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class GeographyIntegrationTest extends AbstractIntegrationTest {

    // Far from the coordinates other tests use, so their villages never show up in these nearby searches
    private static final double ANCHOR_LAT = 31.40;
    private static final double ANCHOR_LON = 34.40;

    @Test
    void listGovernorates_isPublic_andIncludesSeededGovernorates() throws Exception {
        ApiResponse response = call("GET", "/api/v1/governorates", null, null);

        assertEquals(200, response.status());
        assertTrue(response.body().contains("\"name\":\"Nablus\""));
        assertTrue(response.body().contains("\"name\":\"Hebron\""));
        assertTrue(response.body().contains("\"name\":\"Rafah\""));
    }

    @Test
    void listMunicipalities_returnsMunicipalitiesOfTheGovernorate() throws Exception {
        long nablusId = jdbcTemplate.queryForObject("SELECT id FROM governorates WHERE name = 'Nablus'", Long.class);

        ApiResponse response = call("GET", "/api/v1/governorates/" + nablusId + "/municipalities", null, null);

        assertEquals(200, response.status());
        assertTrue(response.body().contains("\"name\":\"Nablus\""));
        assertFalse(response.body().contains("\"name\":\"Hebron\""));
    }

    @Test
    void listMunicipalities_forUnknownGovernorate_returns422() throws Exception {
        ApiResponse response = call("GET", "/api/v1/governorates/999999/municipalities", null, null);

        assertEquals(422, response.status());
        assertTrue(response.body().contains("GOVERNORATE_NOT_FOUND"));
    }

    @Test
    void createVillage_withMunicipality_showsMunicipalityName() throws Exception {
        String token = registerUser(uniqueName("geo"));
        long nablusId = governorateId("Nablus");
        long municipalityId = municipalityId("Nablus");

        ApiResponse response = call("POST", "/api/v1/villages", token,
                villageJson(nablusId, 32.30, 35.20, "Village in Nablus municipality", municipalityId));

        assertEquals(201, response.status());
        assertTrue(response.body().contains("\"municipalityName\":\"Nablus\""));
    }

    @Test
    void createVillage_withMunicipalityFromAnotherGovernorate_returns422() throws Exception {
        String token = registerUser(uniqueName("geo"));
        long nablusId = governorateId("Nablus");
        long hebronMunicipalityId = municipalityId("Hebron");

        ApiResponse response = call("POST", "/api/v1/villages", token,
                villageJson(nablusId, 32.30, 35.20, "Mismatched municipality", hebronMunicipalityId));

        assertEquals(422, response.status());
        assertTrue(response.body().contains("INVALID_MUNICIPALITY"));
    }

    @Test
    void nearby_returnsVillagesInsideTheRadius_sortedByDistance() throws Exception {
        String token = registerUser(uniqueName("geo"));
        long gazaId = governorateId("Gaza");

        long anchor = createVillage(token, gazaId, ANCHOR_LAT, ANCHOR_LON, "Anchor");
        // 0.009 degrees of latitude is about 1 km, 0.045 about 5 km and 0.27 about 30 km
        long fiveKm = createVillage(token, gazaId, ANCHOR_LAT + 0.045, ANCHOR_LON, "Five km north");
        long oneKm = createVillage(token, gazaId, ANCHOR_LAT + 0.009, ANCHOR_LON, "One km north");
        long thirtyKm = createVillage(token, gazaId, ANCHOR_LAT + 0.27, ANCHOR_LON, "Thirty km north");

        ApiResponse response = call("GET", "/api/v1/villages/" + anchor + "/nearby?radiusKm=10", null, null);

        assertEquals(200, response.status());
        String body = response.body();
        int oneKmAt = body.indexOf("\"id\":" + oneKm + ",");
        int fiveKmAt = body.indexOf("\"id\":" + fiveKm + ",");
        assertTrue(oneKmAt >= 0, "the 1 km village should be found: " + body);
        assertTrue(fiveKmAt > oneKmAt, "the 1 km village should come before the 5 km one: " + body);
        assertFalse(body.contains("\"id\":" + thirtyKm + ","), "30 km is outside the radius: " + body);
        assertFalse(body.contains("\"id\":" + anchor + ","), "a village is not its own neighbour: " + body);
        assertTrue(body.contains("\"distanceKm\":1.00"), "Haversine distance of 0.009 degrees: " + body);

        ApiResponse wider = call("GET", "/api/v1/villages/" + anchor + "/nearby?radiusKm=50&limit=2", null, null);
        assertEquals(200, wider.status());
        assertFalse(wider.body().contains("\"id\":" + thirtyKm + ","), "limit=2 keeps only the two nearest");
    }

    @Test
    void nearby_rejectsInvalidRadiusAndUnknownVillage() throws Exception {
        ApiResponse badRadius = call("GET", "/api/v1/villages/1/nearby?radiusKm=0", null, null);
        assertEquals(422, badRadius.status());
        assertTrue(badRadius.body().contains("INVALID_QUERY"));

        ApiResponse notANumber = call("GET", "/api/v1/villages/1/nearby?radiusKm=NaN", null, null);
        assertEquals(422, notANumber.status());
        assertTrue(notANumber.body().contains("INVALID_QUERY"));

        ApiResponse unknown = call("GET", "/api/v1/villages/999999/nearby", null, null);
        assertEquals(422, unknown.status());
        assertTrue(unknown.body().contains("VILLAGE_NOT_FOUND"));
    }

    private long governorateId(String name) {
        return jdbcTemplate.queryForObject("SELECT id FROM governorates WHERE name = ?", Long.class, name);
    }

    private long municipalityId(String name) {
        return jdbcTemplate.queryForObject("SELECT id FROM municipalities WHERE name = ?", Long.class, name);
    }

    private long createVillage(String token, long governorateId, double latitude, double longitude,
                               String description) throws Exception {
        ApiResponse response = call("POST", "/api/v1/villages", token,
                villageJson(governorateId, latitude, longitude, description, null));
        assertEquals(201, response.status(), response.body());
        return extractLong(response.body(), "id");
    }

    private static String villageJson(long governorateId, double latitude, double longitude,
                                      String description, Long municipalityId) {
        // Locale.ROOT so the decimal separator is always a dot, whatever the machine's language settings
        return String.format(Locale.ROOT,
                "{\"governorateId\":%d,\"latitude\":%.6f,\"longitude\":%.6f,"
                        + "\"locationDescription\":\"%s\",\"elevationM\":100,\"municipalityId\":%s}",
                governorateId, latitude, longitude, description, municipalityId);
    }
}
