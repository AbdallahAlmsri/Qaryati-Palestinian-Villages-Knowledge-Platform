package com.qaryati.qaryati.economy;

import com.qaryati.qaryati.history.HistoryReviewRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/villages/{villageId}/economy")
public class VillageEconomyController {

    private final VillageEconomyService economyService;

    public VillageEconomyController(VillageEconomyService economyService) {
        this.economyService = economyService;
    }

    @GetMapping
    public List<VillageEconomyResponse> getEconomy(
            @PathVariable Long villageId) {

        return economyService.getEconomy(villageId)
                .stream()
                .map(VillageEconomyResponse::from)
                .toList();
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VillageEconomyResponse> createEconomy(
            @PathVariable Long villageId,
            @Valid @RequestBody CreateVillageEconomyRequest request) {

        VillageEconomy saved = economyService.createEconomy(
                villageId,
                request.activityName(),
                request.description(),
                request.source()
        );

        return ResponseEntity.status(201)
                .body(VillageEconomyResponse.from(saved));
    }

    @PatchMapping("/{economyId}/review")
    @PreAuthorize("hasAnyRole('VERIFIER', 'ADMIN')")
    public ResponseEntity<VillageEconomyResponse> reviewEconomy(
            @PathVariable Long villageId,
            @PathVariable Long economyId,
            @Valid @RequestBody HistoryReviewRequest request) {

        VillageEconomy reviewed = economyService.reviewEconomy(
                villageId,
                economyId,
                request.status().name()
        );

        return ResponseEntity.ok(
                VillageEconomyResponse.from(reviewed)
        );
    }
}