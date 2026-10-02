package com.qaryati.qaryati.population;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/villages/{villageId}/population")
public class PopulationRecordController {

    private final PopulationRecordService populationRecordService;

    public PopulationRecordController(PopulationRecordService populationRecordService) {
        this.populationRecordService = populationRecordService;
    }

    @GetMapping
    public List<PopulationRecordResponse> getHistory(@PathVariable Long villageId) {
        return populationRecordService.getHistory(villageId)
                .stream()
                .map(PopulationRecordResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<PopulationRecordResponse> createRecord(
            @PathVariable Long villageId,
            @Valid @RequestBody CreatePopulationRecordRequest request
    ) {
        PopulationRecord saved = populationRecordService.createRecord(villageId, request);
        return ResponseEntity.status(201).body(PopulationRecordResponse.from(saved));
    }

    @PatchMapping("/{recordId}/review")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('VERIFIER', 'ADMIN')")
    public ResponseEntity<PopulationRecordResponse> reviewRecord(
            @PathVariable Long villageId,
            @PathVariable Long recordId,
            @Valid @RequestBody ReviewDecisionRequest request
    ) {
        PopulationRecord reviewed = populationRecordService.reviewRecord(villageId, recordId, request);
        return ResponseEntity.ok(PopulationRecordResponse.from(reviewed));
    }
}