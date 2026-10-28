package com.qaryati.qaryati.geocoding;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class NominatimLocationProviderTest {

    @Test
    void geocode_respectsConfiguredRateLimit() throws Exception {
        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);

        List<Long> requestTimes = new ArrayList<>();

        server.createContext("/search", exchange -> {
            requestTimes.add(System.nanoTime());

            byte[] response = "[]".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);

            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });

        server.start();

        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();

            NominatimLocationProvider provider =
                    new NominatimLocationProvider(baseUrl, "ps", 500, 500, 100);

            assertEquals(Optional.empty(), provider.geocode("village-one"));
            assertEquals(Optional.empty(), provider.geocode("village-two"));

            assertEquals(2, requestTimes.size());

            long elapsed = requestTimes.get(1) - requestTimes.get(0);

            assertTrue(
                    elapsed >= TimeUnit.MILLISECONDS.toNanos(90),
                    "Requests must respect the configured rate limit"
            );
        } finally {
            server.stop(0);
        }
    }

    @Test
    void geocode_timesOutWhenProviderIsSlow() throws Exception {
        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);

        server.createContext("/search", exchange -> {
            try {
                Thread.sleep(600);
                byte[] response = "[]".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, response.length);
                try (var output = exchange.getResponseBody()) {
                    output.write(response);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (java.io.IOException ignored) {
            } finally {
                exchange.close();
            }
        });

        server.start();

        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();

            NominatimLocationProvider provider =
                    new NominatimLocationProvider(baseUrl, "ps", 200, 150, 0);

            long start = System.nanoTime();

            assertThrows(
                    org.springframework.web.client.RestClientException.class,
                    () -> provider.geocode("slow-village")
            );

            long elapsed = TimeUnit.NANOSECONDS.toMillis(
                    System.nanoTime() - start);

            assertTrue(elapsed < 2000,
                    "Slow provider must time out promptly");
        } finally {
            server.stop(0);
        }
    }
}