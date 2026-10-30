package com.qaryati.qaryati.geocoding;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class NominatimLocationProvider implements LocationProvider {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(NominatimLocationProvider.class);

    private final RestClient restClient;
    private final String countryCodes;
    private final Object rateLimitLock = new Object();
    private final long rateLimitIntervalNanos;

    private long nextAllowedRequestNanos = 0L;

    public NominatimLocationProvider(
            @Value("${app.geocoding.base-url}") String baseUrl,
            @Value("${app.geocoding.country-codes}") String countryCodes,
            @Value("${app.geocoding.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${app.geocoding.read-timeout-ms:5000}") int readTimeoutMs,
            @Value("${app.geocoding.rate-limit-ms:1000}") long rateLimitMs
    ) {
        this.countryCodes = countryCodes;
        this.rateLimitIntervalNanos = TimeUnit.MILLISECONDS.toNanos(rateLimitMs);

        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(baseUrl)
                .defaultHeader(
                        "User-Agent",
                        "Qaryati-Project/1.0 (university coursework)"
                )
                .build();
    }

    @Override
    @Cacheable(value = "geocodeResults", key = "#query", unless = "#result == null")
    @Retry(name = "nominatim", fallbackMethod = "geocodeFallback")
    @CircuitBreaker(name = "nominatim")
    public Optional<GeocodeResult> geocode(String query) {
        synchronized (rateLimitLock) {
            waitForRateLimit();

            try {
                return performGeocode(query);
            } finally {
                nextAllowedRequestNanos =
                        System.nanoTime() + rateLimitIntervalNanos;
            }
        }
    }

    private Optional<GeocodeResult> performGeocode(String query) {
        List<Map<String, Object>> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("q", query)
                        .queryParam("countrycodes", countryCodes)
                        .queryParam("format", "json")
                        .queryParam("limit", 1)
                        .build())
                .retrieve()
                .body(List.class);

        if (response == null || response.isEmpty()) {
            return Optional.empty();
        }

        Map<String, Object> first = response.get(0);

        return Optional.of(new GeocodeResult(
                new BigDecimal((String) first.get("lat")),
                new BigDecimal((String) first.get("lon")),
                (String) first.get("display_name")
        ));
    }

    private void waitForRateLimit() {
        long remaining = nextAllowedRequestNanos - System.nanoTime();

        if (remaining > 0) {
            try {
                TimeUnit.NANOSECONDS.sleep(remaining);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "Interrupted while waiting for Nominatim rate limit", e
                );
            }
        }
    }

    @SuppressWarnings("unused")
    private Optional<GeocodeResult> geocodeFallback(
            String query,
            Throwable throwable
    ) {
        log.warn(
                "Geocoding fallback triggered for query '{}': {}",
                query,
                throwable.toString()
        );

        return Optional.empty();
    }
}
