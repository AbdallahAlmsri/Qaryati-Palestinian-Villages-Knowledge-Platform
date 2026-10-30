package com.qaryati.qaryati.geocoding;

import com.qaryati.qaryati.governorate.Governorate;
import com.qaryati.qaryati.village.Village;
import com.qaryati.qaryati.village.VillageNotFoundException;
import com.qaryati.qaryati.village.VillageRepository;
import com.qaryati.qaryati.villagename.VillageName;
import com.qaryati.qaryati.villagename.VillageNameRepository;
import com.qaryati.qaryati.villagename.VillageNameStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeocodingServiceTest {

    @Mock
    private VillageRepository villageRepository;

    @Mock
    private VillageNameRepository villageNameRepository;

    @Mock
    private LocationProvider locationProvider;

    @InjectMocks
    private GeocodingService geocodingService;

    private Village villageWith(String lat, String lon) {
        return new Village(
                new Governorate("Nablus"),
                new BigDecimal(lat),
                new BigDecimal(lon),
                "A village in Nablus",
                500
        );
    }

    private void mockArabicName(Long villageId, String name) {
        VillageName villageName = mock(VillageName.class);
        when(villageName.getName()).thenReturn(name);

        when(villageNameRepository
                .findFirstByVillageIdAndLanguageIgnoreCaseAndStatusOrderByIdDesc(
                        villageId, "ar", VillageNameStatus.VERIFIED))
                .thenReturn(Optional.of(villageName));
    }

    @Test
    void checkCoordinates_throwsException_whenVillageDoesNotExist() {
        when(villageRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(VillageNotFoundException.class,
                () -> geocodingService.checkCoordinates(999L));
    }

    @Test
    void checkCoordinates_throws422Exception_whenNoVerifiedArabicName() {
        when(villageRepository.findById(1L))
                .thenReturn(Optional.of(villageWith("32.1521", "35.2571")));

        when(villageNameRepository
                .findFirstByVillageIdAndLanguageIgnoreCaseAndStatusOrderByIdDesc(
                        1L, "ar", VillageNameStatus.VERIFIED))
                .thenReturn(Optional.empty());

        assertThrows(MissingVerifiedArabicNameException.class,
                () -> geocodingService.checkCoordinates(1L));

        verifyNoInteractions(locationProvider);
    }

    @Test
    void checkCoordinates_throwsException_whenProviderReturnsNothing() {
        when(villageRepository.findById(1L))
                .thenReturn(Optional.of(villageWith("32.1521", "35.2571")));

        mockArabicName(1L, "Huwara");

        when(locationProvider.geocode("Huwara"))
                .thenReturn(Optional.empty());

        assertThrows(GeocodingUnavailableException.class,
                () -> geocodingService.checkCoordinates(1L));
    }

    @Test
    void checkCoordinates_reportsMatch_whenWithinTolerance() {
        when(villageRepository.findById(1L))
                .thenReturn(Optional.of(villageWith("32.1521", "35.2571")));

        mockArabicName(1L, "Huwara");

        when(locationProvider.geocode("Huwara")).thenReturn(Optional.of(
                new GeocodeResult(
                        new BigDecimal("32.1521357"),
                        new BigDecimal("35.2571332"),
                        "Huwara"
                )
        ));

        GeocodeCheckResponse result = geocodingService.checkCoordinates(1L);

        assertTrue(result.coordinatesMatch());
        verify(locationProvider).geocode("Huwara");
    }

    @Test
    void checkCoordinates_reportsMismatch_whenOutsideTolerance() {
        when(villageRepository.findById(1L))
                .thenReturn(Optional.of(villageWith("32.2215", "35.2556")));

        mockArabicName(1L, "Huwara");

        when(locationProvider.geocode("Huwara")).thenReturn(Optional.of(
                new GeocodeResult(
                        new BigDecimal("32.1521357"),
                        new BigDecimal("35.2571332"),
                        "Huwara"
                )
        ));

        GeocodeCheckResponse result = geocodingService.checkCoordinates(1L);

        assertFalse(result.coordinatesMatch());
        assertEquals(new BigDecimal("32.1521357"), result.resolvedLatitude());
    }
}