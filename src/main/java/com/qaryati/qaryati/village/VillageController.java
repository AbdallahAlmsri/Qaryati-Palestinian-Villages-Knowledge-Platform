package com.qaryati.qaryati.village;

import com.qaryati.qaryati.governorate.Governorate;
import com.qaryati.qaryati.governorate.GovernorateRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/villages")
public class VillageController {

    private final VillageRepository villageRepository;
    private final GovernorateRepository governorateRepository;

    public VillageController(VillageRepository villageRepository, GovernorateRepository governorateRepository) {
        this.villageRepository = villageRepository;
        this.governorateRepository = governorateRepository;
    }

    @GetMapping
    public List<VillageResponse> getAllVillages() {
        return villageRepository.findAll()
                .stream()
                .map(VillageResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<?> createVillage(@Valid @RequestBody CreateVillageRequest request) {
        Governorate governorate = governorateRepository.findById(request.governorateId())
                .orElse(null);

        if (governorate == null) {
            return ResponseEntity.status(422).body("Governorate with id " + request.governorateId() + " does not exist");
        }

        Village village = new Village(
                governorate,
                request.latitude(),
                request.longitude(),
                request.locationDescription(),
                request.elevationM()
        );

        Village saved = villageRepository.save(village);
        return ResponseEntity.status(201).body(VillageResponse.from(saved));
    }
}