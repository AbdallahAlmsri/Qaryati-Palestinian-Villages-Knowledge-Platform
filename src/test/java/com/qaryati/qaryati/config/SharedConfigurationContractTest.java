package com.qaryati.qaryati.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * application.properties holds the settings every profile shares. The automated tests run with the "test" profile,
 * which supplies its own values, so a setting lost from the shared file would not fail any other test. It would only
 * break the real application: "docker compose up" refused to start once because app.jwt.expiration-ms was missing.
 * This test fails the build if any of these settings disappears from the shared file.
 */
class SharedConfigurationContractTest {

    private static final List<String> REQUIRED = List.of(
            "spring.application.name",
            "spring.profiles.default",
            "spring.jpa.open-in-view",
            "app.jwt.expiration-ms",
            "app.cors.allowed-origins",
            "app.rate-limit.auth.max-requests",
            "app.rate-limit.auth.window-seconds",
            "app.geocoding.base-url",
            "app.geocoding.country-codes",
            "app.geocoding.connect-timeout-ms",
            "app.geocoding.read-timeout-ms",
            "app.geocoding.rate-limit-ms",
            "resilience4j.retry.instances.nominatim.max-attempts",
            "resilience4j.circuitbreaker.instances.nominatim.failure-rate-threshold",
            "spring.cache.caffeine.spec",
            "logging.pattern.level"
    );

    @Test
    void sharedPropertiesDefineEverySettingTheApplicationNeeds() throws IOException {
        Properties properties = new Properties();
        try (InputStream in = new ClassPathResource("application.properties").getInputStream()) {
            properties.load(in);
        }

        for (String key : REQUIRED) {
            assertTrue(properties.containsKey(key),
                    "application.properties must define '" + key + "' (it is needed by the dev or prod profile)");
        }
    }
}
