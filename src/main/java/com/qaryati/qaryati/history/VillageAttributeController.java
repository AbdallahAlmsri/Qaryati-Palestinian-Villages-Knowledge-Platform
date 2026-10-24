package com.qaryati.qaryati.history;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/villages/{villageId}/attributes")
public class VillageAttributeController {

    private final VillageAttributeService villageAttributeService;

    public VillageAttributeController(VillageAttributeService villageAttributeService) {
        this.villageAttributeService = villageAttributeService;
    }

    @GetMapping
    public List<VillageAttributeResponse> getAttributes(@PathVariable Long villageId) {
        return villageAttributeService.getAttributes(villageId)
                .stream()
                .map(VillageAttributeResponse::from)
                .toList();
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VillageAttributeResponse> createAttribute(
            @PathVariable Long villageId,
            @Valid @RequestBody CreateVillageAttributeRequest request
    ) {
        VillageAttribute saved = villageAttributeService.createAttribute(villageId, request);

        return ResponseEntity.status(201)
                .body(VillageAttributeResponse.from(saved));
    }

    @PatchMapping("/{attributeId}/review")
    @PreAuthorize("hasAnyRole('VERIFIER', 'ADMIN')")
    public ResponseEntity<VillageAttributeResponse> reviewAttribute(
            @PathVariable Long villageId,
            @PathVariable Long attributeId,
            @Valid @RequestBody HistoryReviewRequest request) {

        VillageAttribute attribute = villageAttributeService.reviewAttribute(villageId, attributeId, request);

        return ResponseEntity.ok(VillageAttributeResponse.from(attribute));
    }
}