package com.qaryati.qaryati.population;

import com.qaryati.qaryati.user.User;
import com.qaryati.qaryati.user.UserRepository;
import com.qaryati.qaryati.village.Village;
import com.qaryati.qaryati.village.VillageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PopulationRecordServiceTest {

    @Mock
    private PopulationRecordRepository populationRecordRepository;

    @Mock
    private VillageRepository villageRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PopulationRecordService populationRecordService;

    @Test
    void createRecord_throwsException_whenVillageDoesNotExist() {
        when(villageRepository.findById(999L)).thenReturn(Optional.empty());

        CreatePopulationRecordRequest request = new CreatePopulationRecordRequest(
                4500, 2020, "PCBS Census 2020", null, null
        );

        assertThrows(VillageNotFoundException.class,
                () -> populationRecordService.createRecord(999L, request));
        verify(populationRecordRepository, never()).save(any());
    }

    @Test
    void createRecord_savesRecord_whenVillageExistsAndUserIsAuthenticated() {
        Village village = new Village();
        User user = new User("abdallah", "abdallah@example.com", "hashed", com.qaryati.qaryati.user.Role.CONTRIBUTOR);

        var auth = new UsernamePasswordAuthenticationToken("abdallah", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(villageRepository.findById(1L)).thenReturn(Optional.of(village));
        when(userRepository.findByUsername("abdallah")).thenReturn(Optional.of(user));
        when(populationRecordRepository.save(any(PopulationRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreatePopulationRecordRequest request = new CreatePopulationRecordRequest(
                4500, 2020, "PCBS Census 2020", null, null
        );

        PopulationRecord result = populationRecordService.createRecord(1L, request);

        assertNotNull(result);
        assertEquals(PopulationStatus.PENDING, result.getStatus());
        verify(populationRecordRepository).save(any(PopulationRecord.class));

        SecurityContextHolder.clearContext();
    }

    @Test
    void getHistory_throwsException_whenVillageDoesNotExist() {
        when(villageRepository.existsById(999L)).thenReturn(false);

        assertThrows(VillageNotFoundException.class,
                () -> populationRecordService.getHistory(999L));
    }
}