package com.qaryati.qaryati.village;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/villages")
public class VillageController {

    private final VillageRepository villageRepository;

    public VillageController(VillageRepository villageRepository) {
        this.villageRepository = villageRepository;
    }

    @GetMapping
    public List<VillageResponse> getAllVillages() {
        return villageRepository.findAll()
                .stream()
                .map(VillageResponse::from)
                .toList();
    }
}