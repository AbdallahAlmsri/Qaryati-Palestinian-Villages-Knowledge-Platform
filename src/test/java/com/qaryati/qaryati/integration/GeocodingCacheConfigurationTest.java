package com.qaryati.qaryati.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ADR 006: the geocoding cache must expire entries and must be bounded. Without the setting in
 * application.properties, Caffeine would keep every entry forever.
 */
class GeocodingCacheConfigurationTest extends AbstractIntegrationTest {

    @Autowired
    private CacheManager cacheManager;

    @Test
    void geocodeResultsCache_expiresEntriesAndIsBounded() {
        CaffeineCache cache = (CaffeineCache) cacheManager.getCache("geocodeResults");
        assertNotNull(cache, "the geocodeResults cache must exist");

        var policy = cache.getNativeCache().policy();

        assertEquals(24, policy.expireAfterWrite().orElseThrow().getExpiresAfter(TimeUnit.HOURS),
                "entries must expire after 24 hours");
        assertEquals(1000L, policy.eviction().orElseThrow().getMaximum(),
                "the cache must hold at most 1000 entries");
    }
}
