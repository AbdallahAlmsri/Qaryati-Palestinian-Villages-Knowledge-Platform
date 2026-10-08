package com.qaryati.qaryati.geocoding;

import com.qaryati.qaryati.village.Village;
import com.qaryati.qaryati.village.VillageRepository;
import com.qaryati.qaryati.village.VillageNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class GeocodingService {

    private static final BigDecimal TOLERANCE_DEGREES = new BigDecimal("0.01");

    private final VillageRepository villageRepository;
    private final LocationProvider locationProvider;

    public GeocodingService(VillageRepository villageRepository, LocationProvider locationProvider) {
        this.villageRepository = villageRepository;
        this.locationProvider = locationProvider;
    }

    public GeocodeCheckResponse checkCoordinates(Long villageId) {
        Village village = villageRepository.findById(villageId)
                .orElseThrow(() -> new VillageNotFoundException(villageId));

        if (village.getLocationDescription() == null || village.getLocationDescription().isBlank()) {
            throw new GeocodingUnavailableException(
                    "Village " + villageId + " has no location description to geocode against"
            );
        }

        GeocodeResult result = locationProvider.geocode(village.getLocationDescription())
                .orElseThrow(() -> new GeocodingUnavailableException(
                        "Could not resolve coordinates for village " + villageId + " from external provider"
                ));

        boolean matches = isWithinTolerance(village.getLatitude(), result.latitude())
                && isWithinTolerance(village.getLongitude(), result.longitude());

        String message = matches
                ? "Stored coordinates are consistent with the geocoding provider"
                : "Stored coordinates differ from the geocoding provider by more than the tolerance";

        return new GeocodeCheckResponse(
                villageId,
                village.getLatitude(),
                village.getLongitude(),
                result.latitude(),
                result.longitude(),
                result.displayName(),
                matches,
                message
        );
    }

    private boolean isWithinTolerance(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return false;
        return a.subtract(b).abs().compareTo(TOLERANCE_DEGREES) <= 0;
    }
}