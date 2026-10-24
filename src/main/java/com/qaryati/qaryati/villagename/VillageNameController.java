package com.qaryati.qaryati.villagename;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/villages/{villageId}/names")
public class VillageNameController {

    private final VillageNameService villageNameService;

    public VillageNameController(VillageNameService villageNameService) {
        this.villageNameService = villageNameService;
    }

    @GetMapping
    public List<VillageNameResponse> getNames(@PathVariable Long villageId) {
        return villageNameService.getNames(villageId);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VillageNameResponse> createName(
            @PathVariable Long villageId,
            @Valid @RequestBody VillageNameRequest request
    ) {
        VillageNameResponse saved = villageNameService.createName(villageId, request);
        return ResponseEntity.status(201).body(saved);
    }
}