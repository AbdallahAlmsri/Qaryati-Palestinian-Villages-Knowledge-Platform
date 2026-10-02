package com.qaryati.qaryati.village;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/villages")
public class VillageController {

    private final VillageService villageService;

    public VillageController(VillageService villageService) {
        this.villageService = villageService;
    }

    @GetMapping
    public List<VillageResponse> getAllVillages() {
        return villageService.getAllVillages()
                .stream()
                .map(VillageResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<VillageResponse> createVillage(@Valid @RequestBody CreateVillageRequest request) {
        Village saved = villageService.createVillage(request);
        return ResponseEntity.status(201).body(VillageResponse.from(saved));
    }
}