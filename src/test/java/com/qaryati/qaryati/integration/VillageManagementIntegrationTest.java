package com.qaryati.qaryati.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VillageManagementIntegrationTest extends AbstractIntegrationTest {

    private static final String VILLAGES_API = "/api/v1/villages";

    private static final String VILLAGE_JSON =
            "{\"governorateId\":1,\"latitude\":32.25,\"longitude\":35.3,"
                    + "\"locationDescription\":\"Management test village\",\"elevationM\":600}";

    private static final String UPDATE_JSON =
            "{\"governorateId\":1,\"latitude\":32.5,\"longitude\":35.6,"
                    + "\"locationDescription\":\"Updated village location\",\"elevationM\":750}";

    private String registerAdmin(String username) throws Exception {
        registerUser(username);

        jdbcTemplate.update(
                "UPDATE users SET role = 'ADMIN' WHERE username = ?",
                username
        );

        return login(username);
    }

    private long createVillage(String token) throws Exception {
        ApiResponse response = call(
                "POST", VILLAGES_API, token, VILLAGE_JSON
        );

        assertEquals(201, response.status(), response.body());
        return extractLong(response.body(), "id");
    }

    private ApiResponse updateVillage(
            String token, long villageId, String json
    ) throws Exception {
        return call(
                "PUT", VILLAGES_API + "/" + villageId, token, json
        );
    }

    private ApiResponse deleteVillage(
            String token, long villageId
    ) throws Exception {
        return call(
                "DELETE", VILLAGES_API + "/" + villageId, token, null
        );
    }

    @Test
    void adminCanUpdateVillage() throws Exception {
        String token = registerAdmin(uniqueName("admin"));
        long villageId = createVillage(token);

        ApiResponse response = updateVillage(
                token, villageId, UPDATE_JSON
        );

        assertEquals(200, response.status(), response.body());
        assertTrue(response.body().contains("Updated village location"));
        assertTrue(response.body().contains("\"elevationM\":750"));

        String savedDescription = jdbcTemplate.queryForObject(
                "SELECT location_description FROM villages WHERE id = ?",
                String.class,
                villageId
        );

        assertEquals("Updated village location", savedDescription);
    }

    @Test
    void viewerCannotUpdateVillage() throws Exception {
        String adminToken = registerAdmin(uniqueName("admin"));
        long villageId = createVillage(adminToken);
        String viewerToken = registerUser(uniqueName("viewer"));

        ApiResponse response = updateVillage(
                viewerToken, villageId, UPDATE_JSON
        );

        assertEquals(403, response.status(), response.body());
    }

    @Test
    void unauthenticatedUserCannotUpdateVillage() throws Exception {
        String adminToken = registerAdmin(uniqueName("admin"));
        long villageId = createVillage(adminToken);

        ApiResponse response = updateVillage(
                null, villageId, UPDATE_JSON
        );

        assertEquals(401, response.status(), response.body());
    }

    @Test
    void updatingMissingVillageReturnsNotFound() throws Exception {
        String token = registerAdmin(uniqueName("admin"));

        ApiResponse response = updateVillage(
                token, Long.MAX_VALUE, UPDATE_JSON
        );

        assertEquals(422, response.status(), response.body());
    }

    @Test
    void invalidUpdateReturnsValidationError() throws Exception {
        String token = registerAdmin(uniqueName("admin"));
        long villageId = createVillage(token);

        String invalidJson =
                "{\"governorateId\":1,\"latitude\":100,"
                        + "\"longitude\":35.6,\"elevationM\":750}";

        ApiResponse response = updateVillage(
                token, villageId, invalidJson
        );

        assertEquals(422, response.status(), response.body());
    }

    @Test
    void adminCanDeleteVillage() throws Exception {
        String token = registerAdmin(uniqueName("admin"));
        long villageId = createVillage(token);

        ApiResponse response = deleteVillage(token, villageId);

        assertEquals(204, response.status(), response.body());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM villages WHERE id = ?",
                Integer.class,
                villageId
        );

        assertEquals(0, count);
    }

    @Test
    void viewerCannotDeleteVillage() throws Exception {
        String adminToken = registerAdmin(uniqueName("admin"));
        long villageId = createVillage(adminToken);
        String viewerToken = registerUser(uniqueName("viewer"));

        ApiResponse response = deleteVillage(
                viewerToken, villageId
        );

        assertEquals(403, response.status(), response.body());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM villages WHERE id = ?",
                Integer.class,
                villageId
        );

        assertEquals(1, count);
    }

    @Test
    void deletingMissingVillageReturnsNotFound() throws Exception {
        String token = registerAdmin(uniqueName("admin"));

        ApiResponse response = deleteVillage(
                token, Long.MAX_VALUE
        );

        assertEquals(422, response.status(), response.body());
    }
}
