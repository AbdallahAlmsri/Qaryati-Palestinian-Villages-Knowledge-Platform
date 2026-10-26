package com.qaryati.qaryati.geocoding;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class NominatimLocationProvider implements LocationProvider {

    private final RestClient restClient;
    private final String countryCodes;
    private static final Object RATE_LIMIT_LOCK = new Object();
    private static long nextAllowedRequestNanos = 0L;

    public NominatimLocationProvider(
            @Value("${app.geocoding.base-url}") String baseUrl,
            @Value("${app.geocoding.country-codes}") String countryCodes
    ) {
        this.countryCodes = countryCodes;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(5000);

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", "Qaryati-Project/1.0 (university coursework)")
                .build();
    }

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(NominatimLocationProvider.class);

    @Override
    @Cacheable(value = "geocodeResults", key = "#query", unless = "#result == null")
    @CircuitBreaker(name = "nominatim", fallbackMethod = "geocodeFallback")
    public Optional<GeocodeResult> geocode(String query) {
        synchronized (RATE_LIMIT_LOCK) {
            waitForRateLimit();
            try {
                return performGeocode(query);
            } finally {
                nextAllowedRequestNanos = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(1);
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
        synchronized (RATE_LIMIT_LOCK) {
            long remaining = nextAllowedRequestNanos - System.nanoTime();

            if (remaining > 0) {
                try {
                    java.util.concurrent.TimeUnit.NANOSECONDS.sleep(remaining);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(
                            "Interrupted while waiting for Nominatim rate limit", e);
                }
            }

        }
    }
    @SuppressWarnings("unused")
    private Optional<GeocodeResult> geocodeFallback(String query, Throwable throwable) {
        log.warn("Geocoding fallback triggered for query '{}': {}", query, throwable.toString());
        return Optional.empty();
    }
}