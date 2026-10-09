package com.qaryati.qaryati.village;

import com.qaryati.qaryati.governorate.Governorate;
import com.qaryati.qaryati.governorate.GovernorateRepository;
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
}