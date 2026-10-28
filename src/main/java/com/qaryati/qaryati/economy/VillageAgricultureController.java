package com.qaryati.qaryati.economy;

import com.qaryati.qaryati.history.HistoryReviewRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/villages/{villageId}/agriculture")
public class VillageAgricultureController {

    private final VillageAgricultureService agricultureService;

    public VillageAgricultureController(
            VillageAgricultureService agricultureService) {
        this.agricultureService = agricultureService;
    }

    @GetMapping
    public List<VillageAgricultureResponse> getAgriculture(
            @PathVariable Long villageId) {

        return agricultureService.getAgriculture(villageId)
                .stream()
                .map(VillageAgricultureResponse::from)
                .toList();
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VillageAgricultureResponse> createAgriculture(
            @PathVariable Long villageId,
            @Valid @RequestBody CreateVillageAgricultureRequest request) {

        VillageAgriculture saved = agricultureService.createAgriculture(
                villageId,
                request.cropName(),
                request.description(),
                request.source()
        );

        return ResponseEntity.status(201)
                .body(VillageAgricultureResponse.from(saved));
    }

    @PatchMapping("/{agricultureId}/review")
    @PreAuthorize("hasAnyRole('VERIFIER', 'ADMIN')")
    public ResponseEntity<VillageAgricultureResponse> reviewAgriculture(
            @PathVariable Long villageId,
            @PathVariable Long agricultureId,
            @Valid @RequestBody HistoryReviewRequest request) {

        VillageAgriculture reviewed = agricultureService.reviewAgriculture(
                villageId,
                agricultureId,
                request.status().name()
        );

        return ResponseEntity.ok(
                VillageAgricultureResponse.from(reviewed)
        );
    }
}