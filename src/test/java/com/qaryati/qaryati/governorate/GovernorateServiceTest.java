package com.qaryati.qaryati.governorate;

import com.qaryati.qaryati.village.GovernorateNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GovernorateServiceTest {

    @Mock
    private GovernorateRepository governorateRepository;

    @Mock
    private MunicipalityRepository municipalityRepository;

    @InjectMocks
    private GovernorateService governorateService;

    @Test
    void getGovernorates_returnsThemSortedByName() {
        List<Governorate> governorates = List.of(new Governorate("Jenin"), new Governorate("Nablus"));
        when(governorateRepository.findAll(Sort.by("name"))).thenReturn(governorates);

        assertEquals(governorates, governorateService.getGovernorates());
    }

    @Test
    void getMunicipalities_throwsWhenGovernorateDoesNotExist() {
        when(governorateRepository.existsById(999L)).thenReturn(false);

        assertThrows(GovernorateNotFoundException.class, () -> governorateService.getMunicipalities(999L));
        verifyNoInteractions(municipalityRepository);
    }

    @Test
    void getMunicipalities_returnsMunicipalitiesOfTheGovernorate() {
        Governorate nablus = new Governorate("Nablus");
        List<Municipality> municipalities = List.of(new Municipality(nablus, "Nablus"));
        when(governorateRepository.existsById(1L)).thenReturn(true);
        when(municipalityRepository.findByGovernorateIdOrderByNameAsc(1L)).thenReturn(municipalities);

        assertEquals(municipalities, governorateService.getMunicipalities(1L));
    }
}
