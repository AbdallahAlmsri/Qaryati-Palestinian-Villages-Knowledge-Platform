package com.qaryati.qaryati.village;

import com.qaryati.qaryati.governorate.Governorate;
import com.qaryati.qaryati.governorate.GovernorateRepository;
import com.qaryati.qaryati.governorate.InvalidMunicipalityException;
import com.qaryati.qaryati.governorate.Municipality;
import com.qaryati.qaryati.governorate.MunicipalityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.qaryati.qaryati.common.InvalidQueryException;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VillageServiceTest {

    @Mock
    private VillageRepository villageRepository;

    @Mock
    private GovernorateRepository governorateRepository;

    @Mock
    private MunicipalityRepository municipalityRepository;

    @InjectMocks
    private VillageService villageService;

    @Test
    void createVillage_throwsException_whenGovernorateDoesNotExist() {
        CreateVillageRequest request = new CreateVillageRequest(
                999L, BigDecimal.valueOf(32.0), BigDecimal.valueOf(35.0), "Test", 500, null
        );

        when(governorateRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(GovernorateNotFoundException.class, () -> villageService.createVillage(request));
        verify(villageRepository, never()).save(any());
    }

    @Test
    void createVillage_savesVillage_whenGovernorateExists() {
        Governorate governorate = new Governorate("Nablus");        CreateVillageRequest request = new CreateVillageRequest(
                1L, BigDecimal.valueOf(32.0), BigDecimal.valueOf(35.0), "Test", 500, null
        );

        when(governorateRepository.findById(1L)).thenReturn(Optional.of(governorate));
        when(villageRepository.save(any(Village.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Village result = villageService.createVillage(request);

        assertNotNull(result);
        verify(villageRepository).save(any(Village.class));
    }

    @Test
    void search_rejectsPageSizeAboveMaximum() {
        assertThrows(InvalidQueryException.class,
                () -> villageService.search(null, null, 0, 101, "id,asc"));
    }

    @Test
    void search_rejectsUnknownSortField() {
        assertThrows(InvalidQueryException.class,
                () -> villageService.search(null, null, 0, 20, "password,asc"));
    }

    @Test
    void search_rejectsBadSortDirection() {
        assertThrows(InvalidQueryException.class,
                () -> villageService.search(null, null, 0, 20, "id,sideways"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_buildsPageRequestWithStableSort() {
        when(villageRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        villageService.search(1L, "nablus", 2, 10, "elevationM,desc");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(villageRepository).findAll(any(Specification.class), captor.capture());
        Pageable pageable = captor.getValue();

        assertEquals(2, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());
        assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("elevationM").getDirection());
        assertNotNull(pageable.getSort().getOrderFor("id"));
    }

    @Test
    void createVillage_linksMunicipality_whenItIsInTheSameGovernorate() {
        Governorate nablus = governorateWithId(1L);
        Municipality municipality = new Municipality(nablus, "Nablus");
        CreateVillageRequest request = new CreateVillageRequest(
                1L, BigDecimal.valueOf(32.0), BigDecimal.valueOf(35.0), "Test", 500, 7L
        );

        when(governorateRepository.findById(1L)).thenReturn(Optional.of(nablus));
        when(municipalityRepository.findById(7L)).thenReturn(Optional.of(municipality));
        when(villageRepository.save(any(Village.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Village result = villageService.createVillage(request);

        assertSame(municipality, result.getMunicipality());
    }

    @Test
    void createVillage_rejectsMunicipalityFromAnotherGovernorate() {
        Governorate nablus = governorateWithId(1L);
        Municipality jeninMunicipality = new Municipality(governorateWithId(2L), "Jenin");
        CreateVillageRequest request = new CreateVillageRequest(
                1L, BigDecimal.valueOf(32.0), BigDecimal.valueOf(35.0), "Test", 500, 7L
        );

        when(governorateRepository.findById(1L)).thenReturn(Optional.of(nablus));
        when(municipalityRepository.findById(7L)).thenReturn(Optional.of(jeninMunicipality));

        assertThrows(InvalidMunicipalityException.class, () -> villageService.createVillage(request));
        verify(villageRepository, never()).save(any());
    }

    @Test
    void createVillage_rejectsUnknownMunicipality() {
        CreateVillageRequest request = new CreateVillageRequest(
                1L, BigDecimal.valueOf(32.0), BigDecimal.valueOf(35.0), "Test", 500, 999L
        );

        Governorate nablus = governorateWithId(1L);
        when(governorateRepository.findById(1L)).thenReturn(Optional.of(nablus));
        when(municipalityRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(InvalidMunicipalityException.class, () -> villageService.createVillage(request));
        verify(villageRepository, never()).save(any());
    }

    @Test
    void findNearby_rejectsRadiusOutOfRange() {
        assertThrows(InvalidQueryException.class, () -> villageService.findNearby(1L, 0, 20));
        assertThrows(InvalidQueryException.class, () -> villageService.findNearby(1L, -5, 20));
        assertThrows(InvalidQueryException.class, () -> villageService.findNearby(1L, 101, 20));
        verifyNoInteractions(villageRepository);
    }

    @Test
    void findNearby_rejectsLimitOutOfRange() {
        assertThrows(InvalidQueryException.class, () -> villageService.findNearby(1L, 10, 0));
        assertThrows(InvalidQueryException.class, () -> villageService.findNearby(1L, 10, 101));
        verifyNoInteractions(villageRepository);
    }

    @Test
    void findNearby_throwsWhenVillageDoesNotExist() {
        when(villageRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(VillageNotFoundException.class, () -> villageService.findNearby(999L, 10, 20));
    }

    @Test
    void findNearby_rejectsVillageWithoutCoordinates() {
        Village noCoordinates = new Village(governorateWithId(1L), null, null, "No location", null);
        when(villageRepository.findById(5L)).thenReturn(Optional.of(noCoordinates));

        assertThrows(InvalidQueryException.class, () -> villageService.findNearby(5L, 10, 20));
    }

    @Test
    void findNearby_searchesABoundingBoxAroundTheVillage() {
        Village village = new Village(governorateWithId(1L),
                BigDecimal.valueOf(32.0), BigDecimal.valueOf(35.0), "Centre", 500);
        when(villageRepository.findById(5L)).thenReturn(Optional.of(village));

        villageService.findNearby(5L, 11.1045, 20);

        // 11.1045 km is 0.1 degree of latitude; at 32°N a degree of longitude is shorter, so the box is wider
        ArgumentCaptor<Double> minLat = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> maxLat = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> minLon = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> maxLon = ArgumentCaptor.forClass(Double.class);
        verify(villageRepository).findNearby(eq(5L), eq(32.0), eq(35.0),
                minLat.capture(), maxLat.capture(), minLon.capture(), maxLon.capture(), eq(11.1045), eq(20));

        assertEquals(31.9, minLat.getValue(), 1e-9);
        assertEquals(32.1, maxLat.getValue(), 1e-9);
        double expectedLonDelta = 0.1 / Math.cos(Math.toRadians(32.0));
        assertEquals(35.0 - expectedLonDelta, minLon.getValue(), 1e-9);
        assertEquals(35.0 + expectedLonDelta, maxLon.getValue(), 1e-9);
    }

    private static Governorate governorateWithId(Long id) {
        Governorate governorate = mock(Governorate.class);
        lenient().when(governorate.getId()).thenReturn(id);
        return governorate;
    }
}