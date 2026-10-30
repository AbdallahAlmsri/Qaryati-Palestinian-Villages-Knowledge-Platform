package com.qaryati.qaryati.config;

import com.qaryati.qaryati.integration.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Starts the application the way "docker compose up" does: the prod profile, and only the environment variables that
 * docker-compose.yml passes in (DB_URL, DB_USERNAME, DB_PASSWORD, APP_JWT_SECRET, APP_CORS_ALLOWED_ORIGINS). The prod
 * profile has no defaults, so any other setting the application needs must come from application.properties.
 * If one is missing, the context fails to start and this test fails, as the real container did.
 * The database comes from Testcontainers, which takes precedence over the dummy DB_URL below.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "DB_URL=jdbc:postgresql://unused-host:5432/unused",
                "DB_USERNAME=unused",
                "DB_PASSWORD=unused",
                "APP_JWT_SECRET=prod-profile-startup-test-secret-prod-profile-startup-test-secret-0123456789",
                "APP_CORS_ALLOWED_ORIGINS=http://localhost:3000"
        })
@ActiveProfiles("prod")
@Import(TestcontainersConfig.class)
class ProdProfileStartupTest {

    @Autowired
    private Environment environment;

    @Test
    void startsWithOnlyTheVariablesDockerComposeProvides() {
        assertEquals(3_600_000L, environment.getProperty("app.jwt.expiration-ms", Long.class),
                "tokens last one hour by default");
        assertEquals("https://nominatim.openstreetmap.org", environment.getProperty("app.geocoding.base-url"));
        assertEquals("ps", environment.getProperty("app.geocoding.country-codes"));
        assertEquals("false", environment.getProperty("spring.jpa.open-in-view"));
        assertEquals(10, environment.getProperty("app.rate-limit.auth.max-requests", Integer.class));
    }
}
