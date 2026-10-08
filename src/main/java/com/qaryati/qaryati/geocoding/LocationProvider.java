package com.qaryati.qaryati.geocoding;

import java.util.Optional;

public interface LocationProvider {
    Optional<GeocodeResult> geocode(String query);
}