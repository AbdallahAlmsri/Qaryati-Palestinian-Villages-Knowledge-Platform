package com.qaryati.qaryati.geocoding;

import java.math.BigDecimal;

public record GeocodeCheckResponse(
        Long villageId,
        BigDecimal storedLatitude,
        BigDecimal storedLongitude,
        BigDecimal resolvedLatitude,
        BigDecimal resolvedLongitude,
        String resolvedDisplayName,
        boolean coordinatesMatch,
        String message
) {
}