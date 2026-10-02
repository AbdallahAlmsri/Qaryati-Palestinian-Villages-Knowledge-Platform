package com.qaryati.qaryati.village;

import com.qaryati.qaryati.governorate.Governorate;
import com.qaryati.qaryati.governorate.GovernorateRepository;
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
class VillageServiceTest {

    @Mock
    private VillageRepository villageRepository;

    @Mock
    private GovernorateRepository governorateRepository;

    @InjectMocks
    private VillageService villageService;

    @Test
    void createVillage_throwsException_whenGovernorateDoesNotExist() {
        CreateVillageRequest request = new CreateVillageRequest(
                999L, BigDecimal.valueOf(32.0), BigDecimal.valueOf(35.0), "Test", 500
        );

        when(governorateRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(GovernorateNotFoundException.class, () -> villageService.createVillage(request));
        verify(villageRepository, never()).save(any());
    }

    @Test
    void createVillage_savesVillage_whenGovernorateExists() {
        Governorate governorate = new Governorate("Nablus");        CreateVillageRequest request = new CreateVillageRequest(
                1L, BigDecimal.valueOf(32.0), BigDecimal.valueOf(35.0), "Test", 500
        );

        when(governorateRepository.findById(1L)).thenReturn(Optional.of(governorate));
        when(villageRepository.save(any(Village.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Village result = villageService.createVillage(request);

        assertNotNull(result);
        verify(villageRepository).save(any(Village.class));
    }
}