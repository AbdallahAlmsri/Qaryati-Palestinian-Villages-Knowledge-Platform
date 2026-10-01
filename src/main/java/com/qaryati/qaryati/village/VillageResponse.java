package com.qaryati.qaryati.village;

import java.math.BigDecimal;

public record VillageResponse(
        Long id,
        String governorateName,
        BigDecimal latitude,
        BigDecimal longitude,
        String locationDescription,
        Integer elevationM
) {
    public static VillageResponse from(Village village) {
        return new VillageResponse(
                village.getId(),
                village.getGovernorate().getName(),
                village.getLatitude(),
                village.getLongitude(),
                village.getLocationDescription(),
                village.getElevationM()
        );
    }
}