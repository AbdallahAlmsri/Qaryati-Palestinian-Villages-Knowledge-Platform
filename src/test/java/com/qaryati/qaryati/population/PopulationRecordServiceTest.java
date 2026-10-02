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

    @Test
    void reviewRecord_throwsException_whenRecordDoesNotExist() {
        when(populationRecordRepository.findById(999L)).thenReturn(Optional.empty());

        ReviewDecisionRequest request = new ReviewDecisionRequest(PopulationStatus.VERIFIED, "notes");

        assertThrows(InvalidReviewException.class,
                () -> populationRecordService.reviewRecord(1L, 999L, request));
    }

    @Test
    void reviewRecord_throwsException_whenAlreadyReviewed() {
        Village village = new Village();
        User creator = new User("abdallah", "abdallah@example.com", "hashed", com.qaryati.qaryati.user.Role.CONTRIBUTOR);
        PopulationRecord record = new PopulationRecord(village, 2020, 4500, "PCBS", null, null, creator);
        record.review(PopulationStatus.VERIFIED, creator);

        when(populationRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        ReviewDecisionRequest request = new ReviewDecisionRequest(PopulationStatus.REJECTED, "too late");

        assertThrows(InvalidReviewException.class,
                () -> populationRecordService.reviewRecord(village.getId(), 1L, request));
    }

    @Test
    void reviewRecord_succeeds_whenPendingAndReviewerAuthenticated() {
        Village village = new Village();
        User creator = new User("abdallah", "abdallah@example.com", "hashed", com.qaryati.qaryati.user.Role.CONTRIBUTOR);
        User reviewer = new User("verifier1", "verifier1@example.com", "hashed", com.qaryati.qaryati.user.Role.VERIFIER);
        PopulationRecord record = new PopulationRecord(village, 2020, 4500, "PCBS", null, null, creator);

        var auth = new UsernamePasswordAuthenticationToken("verifier1", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(populationRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(userRepository.findByUsername("verifier1")).thenReturn(Optional.of(reviewer));
        when(populationRecordRepository.save(any(PopulationRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReviewDecisionRequest request = new ReviewDecisionRequest(PopulationStatus.VERIFIED, "looks correct");

        PopulationRecord result = populationRecordService.reviewRecord(village.getId(), 1L, request);

        assertEquals(PopulationStatus.VERIFIED, result.getStatus());
        assertEquals(reviewer, result.getReviewedBy());

        SecurityContextHolder.clearContext();
    }
}