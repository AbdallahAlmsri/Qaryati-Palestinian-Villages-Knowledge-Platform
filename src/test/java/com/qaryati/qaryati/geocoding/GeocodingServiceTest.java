package com.qaryati.qaryati.geocoding;

import com.qaryati.qaryati.governorate.Governorate;
import com.qaryati.qaryati.village.VillageNotFoundException;
import com.qaryati.qaryati.village.Village;
import com.qaryati.qaryati.village.VillageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeocodingServiceTest {

    @Mock
    private VillageRepository villageRepository;

    @Mock
    private LocationProvider locationProvider;

    @InjectMocks
    private GeocodingService geocodingService;

    private Village villageWith(String description, String lat, String lon) {
        return new Village(
                new Governorate("Nablus"),
                new BigDecimal(lat),
                new BigDecimal(lon),
                description,
                500
        );
    }

    @Test
    void checkCoordinates_throwsException_whenVillageDoesNotExist() {
        when(villageRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(VillageNotFoundException.class,
                () -> geocodingService.checkCoordinates(999L));
    }

    @Test
    void checkCoordinates_throwsException_whenNoLocationDescription() {
        Village village = villageWith("  ", "32.1521", "35.2571");
        when(villageRepository.findById(1L)).thenReturn(Optional.of(village));

        assertThrows(GeocodingUnavailableException.class,
                () -> geocodingService.checkCoordinates(1L));
    }

    @Test
    void checkCoordinates_throwsException_whenProviderReturnsNothing() {
        Village village = villageWith("Huwara", "32.1521", "35.2571");
        when(villageRepository.findById(1L)).thenReturn(Optional.of(village));
        when(locationProvider.geocode("Huwara")).thenReturn(Optional.empty());

        assertThrows(GeocodingUnavailableException.class,
                () -> geocodingService.checkCoordinates(1L));
    }

    @Test
    void checkCoordinates_reportsMatch_whenWithinTolerance() {
        Village village = villageWith("Huwara", "32.1521", "35.2571");
        when(villageRepository.findById(1L)).thenReturn(Optional.of(village));
        when(locationProvider.geocode("Huwara")).thenReturn(Optional.of(
                new GeocodeResult(new BigDecimal("32.1521357"), new BigDecimal("35.2571332"), "Huwara")));

        GeocodeCheckResponse result = geocodingService.checkCoordinates(1L);

        assertTrue(result.coordinatesMatch());
    }

    @Test
    void checkCoordinates_reportsMismatch_whenOutsideTolerance() {
        Village village = villageWith("Huwara", "32.2215", "35.2556");
        when(villageRepository.findById(1L)).thenReturn(Optional.of(village));
        when(locationProvider.geocode("Huwara")).thenReturn(Optional.of(
                new GeocodeResult(new BigDecimal("32.1521357"), new BigDecimal("35.2571332"), "Huwara")));

        GeocodeCheckResponse result = geocodingService.checkCoordinates(1L);

        assertFalse(result.coordinatesMatch());
        assertEquals(new BigDecimal("32.1521357"), result.resolvedLatitude());
    }
}