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
class VillageAgricultureServiceTest {
    @Mock private VillageAgricultureRepository repository;
    @Mock private VillageRepository villageRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private VillageAgricultureService service;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRejectsUnknownVillage() {
        when(villageRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(VillageNotFoundException.class,
                () -> service.createAgriculture(1L, "Olives", "Description", "Source"));
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
        when(repository.save(any(VillageAgriculture.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VillageAgriculture result = service.createAgriculture(1L, "Olives", "Description", "Source");

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
                () -> service.reviewAgriculture(1L, 2L, "VERIFIED"));
    }

    @Test
    void reviewRejectsWrongVillage() {
        Village village = mock(Village.class);
        when(village.getId()).thenReturn(3L);
        VillageAgriculture record = new VillageAgriculture(village, "Olives", "Description", "Source", null);
        when(repository.findById(2L)).thenReturn(Optional.of(record));
        assertThrows(InvalidReviewException.class,
                () -> service.reviewAgriculture(1L, 2L, "VERIFIED"));
    }

    @Test
    void reviewRejectsInvalidDecision() {
        Village village = mock(Village.class);
        when(village.getId()).thenReturn(1L);
        VillageAgriculture record = new VillageAgriculture(village, "Olives", "Description", "Source", null);
        when(repository.findById(2L)).thenReturn(Optional.of(record));
        assertThrows(InvalidReviewException.class,
                () -> service.reviewAgriculture(1L, 2L, "INVALID"));
    }

    @Test
    void reviewUpdatesPendingRecord() {
        Village village = mock(Village.class);
        when(village.getId()).thenReturn(1L);
        User reviewer = new User("reviewer", "reviewer@example.com", "hashed", Role.VERIFIER);
        VillageAgriculture record = new VillageAgriculture(village, "Olives", "Description", "Source", null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("reviewer", null, List.of()));
        when(repository.findById(2L)).thenReturn(Optional.of(record));
        when(userRepository.findByUsername("reviewer")).thenReturn(Optional.of(reviewer));
        when(repository.save(record)).thenReturn(record);

        VillageAgriculture result = service.reviewAgriculture(1L, 2L, "VERIFIED");

        assertEquals("VERIFIED", result.getStatus());
        assertSame(reviewer, result.getReviewedBy());
        assertNotNull(result.getReviewedAt());
    }
}
