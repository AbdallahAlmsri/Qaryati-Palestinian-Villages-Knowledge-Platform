package com.qaryati.qaryati.geocoding;

import java.math.BigDecimal;

public record GeocodeResult(
        BigDecimal latitude,
        BigDecimal longitude,
        String displayName
) {
}