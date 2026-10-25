package com.qaryati.qaryati.history;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/villages/{villageId}/landmarks")
public class LandmarkController {

    private final LandmarkService landmarkService;

    public LandmarkController(LandmarkService landmarkService) {
        this.landmarkService = landmarkService;
    }

    @GetMapping
    public List<LandmarkResponse> getLandmarks(@PathVariable Long villageId) {
        return landmarkService.getLandmarks(villageId)
                .stream()
                .map(LandmarkResponse::from)
                .toList();
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LandmarkResponse> createLandmark(
            @PathVariable Long villageId,
            @Valid @RequestBody CreateLandmarkRequest request
    ) {
        Landmark saved = landmarkService.createLandmark(villageId, request);

        return ResponseEntity.status(201)
                .body(LandmarkResponse.from(saved));
    }

    @PatchMapping("/{landmarkId}/review")
    @PreAuthorize("hasAnyRole('VERIFIER', 'ADMIN')")
    public ResponseEntity<LandmarkResponse> reviewLandmark(
            @PathVariable Long villageId,
            @PathVariable Long landmarkId,
            @Valid @RequestBody HistoryReviewRequest request) {

        Landmark landmark = landmarkService.reviewLandmark(villageId, landmarkId, request);

        return ResponseEntity.ok(LandmarkResponse.from(landmark));
    }
}