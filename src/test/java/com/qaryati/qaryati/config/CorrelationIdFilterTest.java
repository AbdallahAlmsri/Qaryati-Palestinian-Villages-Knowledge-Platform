package com.qaryati.qaryati.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void generatesIdWhenHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/villages");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertNotNull(response.getHeader(CorrelationIdFilter.HEADER));
    }

    @Test
    void reusesValidIncomingId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/villages");
        request.addHeader(CorrelationIdFilter.HEADER, "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals("abc-123", response.getHeader(CorrelationIdFilter.HEADER));
    }

    @Test
    void replacesInvalidIncomingId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/villages");
        request.addHeader(CorrelationIdFilter.HEADER, "bad id\nFAKE LOG LINE");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertNotEquals("bad id\nFAKE LOG LINE", response.getHeader(CorrelationIdFilter.HEADER));
    }

    @Test
    void idIsAvailableDuringRequestAndClearedAfterwards() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/villages");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] seenInsideChain = new String[1];
        FilterChain chain = (req, res) -> seenInsideChain[0] = MDC.get(CorrelationIdFilter.MDC_KEY);

        filter.doFilter(request, response, chain);

        assertNotNull(seenInsideChain[0]);
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }
}