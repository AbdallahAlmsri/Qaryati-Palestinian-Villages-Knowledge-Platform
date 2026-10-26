# Geocoding Hardening (T8)

## Overview

This task improves the reliability of Nominatim geocoding and validates village locations using their Arabic names.

## Features

- Configure a 3-second connection timeout.
- Configure a 5-second read timeout.
- Limit Nominatim requests to at most one request per second.
- Use the latest Arabic name from village_names for geocoding.
- Handle missing Arabic names and unavailable geocoding results.
- Preserve the existing coordinate comparison tolerance.

## Implementation

- NominatimLocationProvider configures HTTP timeouts and request rate limiting.
- GeocodingService retrieves the Arabic village name before checking coordinates.
- VillageNameRepository provides the Arabic name lookup.
- Circuit breaker fallback handles provider failures.

## Testing

GeocodingServiceTest contains six unit tests, including a simulated provider failure.

NominatimLocationProviderTest contains two tests covering request rate limiting and slow provider timeouts.

The complete test suite passed with 66 tests, 0 failures, and 0 errors.

Run all tests:

    .\mvnw.cmd test
