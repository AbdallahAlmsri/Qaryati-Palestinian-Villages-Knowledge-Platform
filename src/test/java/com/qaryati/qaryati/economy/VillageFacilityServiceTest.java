package com.qaryati.qaryati.economy;

import com.qaryati.qaryati.common.InvalidReviewException;
import com.qaryati.qaryati.user.*;
import com.qaryati.qaryati.village.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VillageFacilityServiceTest {
    @Mock private VillageFacilityRepository repository;
    @Mock private VillageRepository villageRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private VillageFacilityService service;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRejectsUnknownVillage() {
        when(villageRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(VillageNotFoundException.class,
                () -> service.createFacility(1L, "School", "Education", "Description", "Source"));
        verify(repository, never()).save(any());
    }

    @Test
    void createStartsPendingAndRecordsCreator() {
        Village village = mock(Village.class);
        User creator = new User("creator", "creator@example.com", "hashed", Role.CONTRIBUTOR);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("creator", null, List.of()));
        when(villageRepository.findById(1L)).thenReturn(Optional.of(village));
        when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));
        when(repository.save(any(VillageFacility.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VillageFacility result = service.createFacility(1L, "School", "Education", "Description", "Source");

        assertEquals("PENDING", result.getStatus());
        assertSame(creator, result.getCreatedBy());
        assertNotNull(result.getCreatedAt());
        assertNull(result.getReviewedBy());
        verify(repository).save(result);
    }

    @Test
    void reviewRejectsMissingRecord() {
        when(repository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(InvalidReviewException.class,
                () -> service.reviewFacility(1L, 2L, "VERIFIED"));
    }

    @Test
    void reviewRejectsWrongVillage() {
        Village village = mock(Village.class);
        when(village.getId()).thenReturn(3L);
        VillageFacility record = new VillageFacility(village, "School", "Education", "Description", "Source", null);
        when(repository.findById(2L)).thenReturn(Optional.of(record));
        assertThrows(InvalidReviewException.class,
                () -> service.reviewFacility(1L, 2L, "VERIFIED"));
    }

    @Test
    void reviewRejectsInvalidDecision() {
        Village village = mock(Village.class);
        when(village.getId()).thenReturn(1L);
        VillageFacility record = new VillageFacility(village, "School", "Education", "Description", "Source", null);
        when(repository.findById(2L)).thenReturn(Optional.of(record));
        assertThrows(InvalidReviewException.class,
                () -> service.reviewFacility(1L, 2L, "INVALID"));
    }

    @Test
    void reviewUpdatesPendingRecord() {
        Village village = mock(Village.class);
        when(village.getId()).thenReturn(1L);
        User reviewer = new User("reviewer", "reviewer@example.com", "hashed", Role.VERIFIER);
        VillageFacility record = new VillageFacility(village, "School", "Education", "Description", "Source", null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("reviewer", null, List.of()));
        when(repository.findById(2L)).thenReturn(Optional.of(record));
        when(userRepository.findByUsername("reviewer")).thenReturn(Optional.of(reviewer));
        when(repository.save(record)).thenReturn(record);

        VillageFacility result = service.reviewFacility(1L, 2L, "VERIFIED");

        assertEquals("VERIFIED", result.getStatus());
        assertSame(reviewer, result.getReviewedBy());
        assertNotNull(result.getReviewedAt());
    }
}
