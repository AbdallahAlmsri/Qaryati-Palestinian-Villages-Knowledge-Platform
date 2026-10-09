package com.qaryati.qaryati.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorFormatIntegrationTest extends AbstractIntegrationTest {

    @Test
    void protectedEndpointWithoutToken_returnsJson401() throws Exception {
        ApiResponse response = call("POST", "/api/v1/villages", null, "{}");

        assertEquals(401, response.status());
        assertTrue(response.body().contains("\"code\":\"UNAUTHORIZED\""));
        assertTrue(response.body().contains("correlationId"));
    }

    @Test
    void invalidToken_returnsJson401() throws Exception {
        ApiResponse response = call("POST", "/api/v1/villages", "not-a-real-token", "{}");

        assertEquals(401, response.status());
        assertTrue(response.body().contains("UNAUTHORIZED"));
    }

    @Test
    void wrongRole_returnsJson403() throws Exception {
        String contributor = registerUser(uniqueName("contrib"));

        ApiResponse response = call("PATCH", "/api/v1/villages/1/population/1/review", contributor,
                "{\"status\":\"VERIFIED\"}");

        assertEquals(403, response.status());
        assertTrue(response.body().contains("\"code\":\"FORBIDDEN\""));
    }

    @Test
    void malformedJson_returns400() throws Exception {
        String token = registerUser(uniqueName("contrib"));

        ApiResponse response = call("POST", "/api/v1/villages", token, "{not json");

        assertEquals(400, response.status());
        assertTrue(response.body().contains("MALFORMED_REQUEST"));
    }

    @Test
    void wrongParameterType_returns400() throws Exception {
        ApiResponse response = call("GET", "/api/v1/villages?page=abc", null, null);

        assertEquals(400, response.status());
        assertTrue(response.body().contains("INVALID_PARAMETER"));
    }

    @Test
    void unknownPath_returnsJson404() throws Exception {
        ApiResponse response = call("GET", "/api/v1/villages/1/nothing-here", null, null);

        assertEquals(404, response.status());
        assertTrue(response.body().contains("\"code\":\"NOT_FOUND\""));
    }

    @Test
    void unsupportedMethod_returnsJson405() throws Exception {
        String token = registerUser(uniqueName("contrib"));

        ApiResponse response = call("DELETE", "/api/v1/villages", token, null);

        assertEquals(405, response.status());
        assertTrue(response.body().contains("METHOD_NOT_ALLOWED"));
    }
}