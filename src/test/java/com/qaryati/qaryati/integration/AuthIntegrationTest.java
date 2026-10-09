package com.qaryati.qaryati.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Test
    void registerThenLogin_succeeds() throws Exception {
        String username = uniqueName("auth");
        assertFalse(registerUser(username).isBlank());

        ApiResponse login = call("POST", "/api/v1/auth/login", null, credentials(username, "password123"));

        assertEquals(200, login.status());
        assertTrue(login.body().contains("\"role\":\"CONTRIBUTOR\""));
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        String username = uniqueName("auth");
        registerUser(username);

        ApiResponse login = call("POST", "/api/v1/auth/login", null, credentials(username, "wrong-password"));

        assertEquals(401, login.status());
    }

    @Test
    void register_withDuplicateUsername_returns422() throws Exception {
        String username = uniqueName("auth");
        registerUser(username);

        String json = "{\"username\":\"%s\",\"email\":\"%s@example.com\",\"password\":\"password123\"}"
                .formatted(username, username);
        ApiResponse second = call("POST", "/api/v1/auth/register", null, json);

        assertEquals(422, second.status());
    }
}