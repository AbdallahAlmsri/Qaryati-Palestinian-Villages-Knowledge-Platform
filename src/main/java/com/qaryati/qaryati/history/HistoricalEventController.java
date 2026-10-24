package com.qaryati.qaryati.history;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/villages/{villageId}/historical-events")
public class HistoricalEventController {

    private final HistoricalEventService historicalEventService;

    public HistoricalEventController(HistoricalEventService historicalEventService) {
        this.historicalEventService = historicalEventService;
    }

    @GetMapping
    public List<HistoricalEventResponse> getEvents(@PathVariable Long villageId) {
        return historicalEventService.getEvents(villageId)
                .stream()
                .map(HistoricalEventResponse::from)
                .toList();
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HistoricalEventResponse> createEvent(
            @PathVariable Long villageId,
            @Valid @RequestBody CreateHistoricalEventRequest request
    ) {
        HistoricalEvent saved = historicalEventService.createEvent(villageId, request);

        return ResponseEntity.status(201)
                .body(HistoricalEventResponse.from(saved));
    }

    @PatchMapping("/{eventId}/review")
    @PreAuthorize("hasAnyRole('VERIFIER', 'ADMIN')")
    public ResponseEntity<HistoricalEventResponse> reviewEvent(
            @PathVariable Long villageId,
            @PathVariable Long eventId,
            @Valid @RequestBody HistoryReviewRequest request) {

        HistoricalEvent event = historicalEventService.reviewEvent(villageId, eventId, request);

        return ResponseEntity.ok(HistoricalEventResponse.from(event));
    }
}