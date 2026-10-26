package com.qaryati.qaryati.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoleManagementIntegrationTest extends AbstractIntegrationTest {

    private static final String USERS_API = "/api/v1/users/";

    private long userId(String username) {
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = ?",
                Long.class,
                username
        );
        assertNotNull(id);
        return id;
    }

    private String registerAdmin(String username) throws Exception {
        registerUser(username);

        jdbcTemplate.update(
                "UPDATE users SET role = 'ADMIN' WHERE username = ?",
                username
        );

        return login(username);
    }

    private ApiResponse changeRole(
            String token,
            long targetId,
            String role
    ) throws Exception {
        return call(
                "PATCH",
                USERS_API + targetId + "/role",
                token,
                "{\"role\":\"" + role + "\"}"
        );
    }

    @Test
    void adminCanChangeUserRole() throws Exception {
        String adminToken = registerAdmin(uniqueName("admin"));
        String username = uniqueName("target");

        registerUser(username);
        long targetId = userId(username);

        ApiResponse response = changeRole(
                adminToken, targetId, "VERIFIER"
        );

        assertEquals(200, response.status(), response.body());
        assertTrue(response.body().contains("\"role\":\"VERIFIER\""));

        String savedRole = jdbcTemplate.queryForObject(
                "SELECT role FROM users WHERE id = ?",
                String.class,
                targetId
        );

        assertEquals("VERIFIER", savedRole);
    }

    @Test
    void nonAdminCannotChangeUserRole() throws Exception {
        String viewerToken = registerUser(uniqueName("viewer"));
        String targetUsername = uniqueName("target");

        registerUser(targetUsername);
        long targetId = userId(targetUsername);

        ApiResponse response = changeRole(
                viewerToken, targetId, "ADMIN"
        );

        assertEquals(403, response.status(), response.body());
    }

    @Test
    void unauthenticatedUserCannotChangeRole() throws Exception {
        String username = uniqueName("target");
        registerUser(username);

        ApiResponse response = changeRole(
                null, userId(username), "ADMIN"
        );

        assertEquals(401, response.status(), response.body());
    }

    @Test
    void adminCannotRemoveOwnAdminRole() throws Exception {
        String username = uniqueName("admin");
        String adminToken = registerAdmin(username);

        ApiResponse response = changeRole(
                adminToken, userId(username), "VIEWER"
        );

        assertEquals(409, response.status(), response.body());

        String savedRole = jdbcTemplate.queryForObject(
                "SELECT role FROM users WHERE username = ?",
                String.class,
                username
        );

        assertEquals("ADMIN", savedRole);
    }

    @Test
    void changingMissingUserReturnsNotFound() throws Exception {
        String adminToken = registerAdmin(uniqueName("admin"));

        ApiResponse response = changeRole(
                adminToken, Long.MAX_VALUE, "VIEWER"
        );

        assertEquals(404, response.status(), response.body());
    }

    @Test
    void invalidRoleReturnsValidationError() throws Exception {
        String adminToken = registerAdmin(uniqueName("admin"));
        String username = uniqueName("target");

        registerUser(username);

        ApiResponse response = changeRole(
                adminToken, userId(username), "SUPERADMIN"
        );

        assertEquals(400, response.status(), response.body());
    }

    @Test
    void demotedAdminLosesAccessWithOldToken() throws Exception {
        String firstAdminToken = registerAdmin(uniqueName("admin"));
        String secondAdminUsername = uniqueName("admin");

        String secondAdminToken = registerAdmin(secondAdminUsername);
        long secondAdminId = userId(secondAdminUsername);

        ApiResponse demotion = changeRole(
                firstAdminToken, secondAdminId, "VIEWER"
        );

        assertEquals(200, demotion.status(), demotion.body());

        ApiResponse attempt = changeRole(
                secondAdminToken, secondAdminId, "ADMIN"
        );

        assertEquals(403, attempt.status(), attempt.body());

        String savedRole = jdbcTemplate.queryForObject(
                "SELECT role FROM users WHERE id = ?",
                String.class,
                secondAdminId
        );

        assertEquals("VIEWER", savedRole);
    }
}
