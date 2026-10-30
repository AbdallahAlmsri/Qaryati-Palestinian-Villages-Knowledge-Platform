package com.qaryati.qaryati.integration;

import com.qaryati.qaryati.geocoding.GeocodeResult;
import com.qaryati.qaryati.geocoding.LocationProvider;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The retry and circuit breaker are annotations on NominatimLocationProvider, so they only work through Spring.
 * The unit tests create the provider with "new" and never exercise them. This test runs the real Spring bean
 * against a local server that always answers 500 and counts the requests it receives.
 *
 * Settings (application.properties): 3 attempts per call; the circuit opens after 6 recorded calls with a
 * failure rate of 50% or more. Two calls therefore use 3 + 3 requests, and a third call is rejected by the open
 * circuit before it reaches the server.
 */
class GeocodingResilienceIntegrationTest extends AbstractIntegrationTest {

    private static final AtomicInteger REQUESTS = new AtomicInteger();
    private static final HttpServer SERVER = startServer();

    private static HttpServer startServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/search", exchange -> {
                REQUESTS.incrementAndGet();
                exchange.sendResponseHeaders(500, -1);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void geocodingProperties(DynamicPropertyRegistry registry) {
        registry.add("app.geocoding.base-url", () -> "http://127.0.0.1:" + SERVER.getAddress().getPort());
        // keep the test fast: no waiting between requests or between retry attempts
        registry.add("app.geocoding.rate-limit-ms", () -> "0");
        registry.add("resilience4j.retry.instances.nominatim.wait-duration", () -> "10ms");
    }

    @AfterAll
    static void stopServer() {
        SERVER.stop(0);
    }

    @Autowired
    private LocationProvider locationProvider;

    @Test
    void geocode_retriesFailures_fallsBackToEmpty_andOpensTheCircuit() {
        Optional<GeocodeResult> first = locationProvider.geocode("resilience-test-one");
        assertTrue(first.isEmpty(), "when the provider keeps failing, the fallback returns an empty result");
        assertEquals(3, REQUESTS.get(),
                "one geocode call must make 3 attempts (retry); if this is 1, the retry is not taking effect");

        Optional<GeocodeResult> second = locationProvider.geocode("resilience-test-two");
        assertTrue(second.isEmpty());
        assertEquals(6, REQUESTS.get(), "the second call makes 3 more attempts");

        Optional<GeocodeResult> third = locationProvider.geocode("resilience-test-three");
        assertTrue(third.isEmpty(), "with the circuit open, the fallback still answers");
        assertEquals(6, REQUESTS.get(),
                "the circuit is open after 6 failed calls, so the third call must not reach the server");
    }
}
