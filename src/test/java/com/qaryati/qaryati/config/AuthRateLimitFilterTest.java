package com.qaryati.qaryati.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthRateLimitFilterTest {

    private MockHttpServletResponse call(AuthRateLimitFilter filter, String uri, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void blocksRequestsOverTheLimit() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(2, 60);

        assertEquals(200, call(filter, "/api/v1/auth/login", "10.0.0.1").getStatus());
        assertEquals(200, call(filter, "/api/v1/auth/login", "10.0.0.1").getStatus());
        assertEquals(429, call(filter, "/api/v1/auth/login", "10.0.0.1").getStatus());
    }

    @Test
    void tracksEachClientSeparately() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(1, 60);

        assertEquals(200, call(filter, "/api/v1/auth/login", "10.0.0.1").getStatus());
        assertEquals(429, call(filter, "/api/v1/auth/login", "10.0.0.1").getStatus());
        assertEquals(200, call(filter, "/api/v1/auth/login", "10.0.0.2").getStatus());
    }

    @Test
    void ignoresNonAuthEndpoints() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(1, 60);

        for (int i = 0; i < 5; i++) {
            assertEquals(200, call(filter, "/api/v1/villages", "10.0.0.1").getStatus());
        }
    }
}