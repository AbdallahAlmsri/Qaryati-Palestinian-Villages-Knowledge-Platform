package com.qaryati.qaryati.village;

import com.qaryati.qaryati.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/villages")
public class VillageController {

    private final VillageService villageService;

    public VillageController(VillageService villageService) {
        this.villageService = villageService;
    }

    @GetMapping
    public PageResponse<VillageResponse> getVillages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,asc") String sort,
            @RequestParam(required = false) Long governorateId,
            @RequestParam(required = false) String q
    ) {
        return PageResponse.from(
                villageService.search(governorateId, q, page, size, sort).map(VillageResponse::from)
        );
    }

    @PostMapping
    public ResponseEntity<VillageResponse> createVillage(@Valid @RequestBody CreateVillageRequest request) {
        Village saved = villageService.createVillage(request);
        return ResponseEntity.status(201).body(VillageResponse.from(saved));
    }
}