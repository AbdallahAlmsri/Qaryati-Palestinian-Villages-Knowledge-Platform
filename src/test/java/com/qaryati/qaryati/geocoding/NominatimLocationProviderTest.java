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
    void geocode_waitsAtLeastOneSecondBetweenRequests() throws Exception {
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
                    new NominatimLocationProvider(baseUrl, "ps");

            assertEquals(Optional.empty(), provider.geocode("village-one"));
            assertEquals(Optional.empty(), provider.geocode("village-two"));

            assertEquals(2, requestTimes.size());

            long elapsed = requestTimes.get(1) - requestTimes.get(0);
            System.out.println("Actual interval: " + TimeUnit.NANOSECONDS.toMillis(elapsed) + " ms");

            assertTrue(
                    elapsed >= TimeUnit.MILLISECONDS.toNanos(950),
                    "Nominatim requests must be spaced about one second apart"
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
                Thread.sleep(7000);
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
                    new NominatimLocationProvider(baseUrl, "ps");

            long start = System.nanoTime();

            assertThrows(
                    org.springframework.web.client.RestClientException.class,
                    () -> provider.geocode("slow-village")
            );

            long elapsed = TimeUnit.NANOSECONDS.toMillis(
                    System.nanoTime() - start);

            System.out.println("Timeout elapsed: " + elapsed + " ms");

            assertTrue(elapsed < 6500,
                    "Slow provider must time out instead of hanging");
        } finally {
            server.stop(0);
        }
    }
}
