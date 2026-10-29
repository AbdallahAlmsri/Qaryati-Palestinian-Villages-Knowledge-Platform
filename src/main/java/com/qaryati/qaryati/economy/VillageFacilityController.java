package com.qaryati.qaryati.economy;

import com.qaryati.qaryati.history.HistoryReviewRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/villages/{villageId}/facilities")
public class VillageFacilityController {

    private final VillageFacilityService facilityService;

    public VillageFacilityController(VillageFacilityService facilityService) {
        this.facilityService = facilityService;
    }

    @GetMapping
    public List<VillageFacilityResponse> getFacilities(
            @PathVariable Long villageId) {

        return facilityService.getFacilities(villageId)
                .stream()
                .map(VillageFacilityResponse::from)
                .toList();
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VillageFacilityResponse> createFacility(
            @PathVariable Long villageId,
            @Valid @RequestBody CreateVillageFacilityRequest request) {

        VillageFacility saved = facilityService.createFacility(
                villageId,
                request.facilityName(),
                request.facilityType(),
                request.description(),
                request.source()
        );

        return ResponseEntity.status(201)
                .body(VillageFacilityResponse.from(saved));
    }

    @PatchMapping("/{facilityId}/review")
    @PreAuthorize("hasAnyRole('VERIFIER', 'ADMIN')")
    public ResponseEntity<VillageFacilityResponse> reviewFacility(
            @PathVariable Long villageId,
            @PathVariable Long facilityId,
            @Valid @RequestBody HistoryReviewRequest request) {

        VillageFacility reviewed = facilityService.reviewFacility(
                villageId,
                facilityId,
                request.status().name()
        );

        return ResponseEntity.ok(
                VillageFacilityResponse.from(reviewed)
        );
    }
}
