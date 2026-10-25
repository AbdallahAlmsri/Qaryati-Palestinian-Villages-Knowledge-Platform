package com.qaryati.qaryati.village;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record NearbyVillageResponse(
        Long id,
        String governorateName,
        BigDecimal latitude,
        BigDecimal longitude,
        String locationDescription,
        BigDecimal distanceKm
) {
    public static NearbyVillageResponse from(NearbyVillageRow row) {
        return new NearbyVillageResponse(
                row.getId(),
                row.getGovernorateName(),
                row.getLatitude(),
                row.getLongitude(),
                row.getLocationDescription(),
                BigDecimal.valueOf(row.getDistanceKm()).setScale(2, RoundingMode.HALF_UP)
        );
    }
}
