package com.qaryati.qaryati.village;

import com.qaryati.qaryati.governorate.Governorate;
import com.qaryati.qaryati.governorate.GovernorateRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VillageService {

    private final VillageRepository villageRepository;
    private final GovernorateRepository governorateRepository;

    public VillageService(VillageRepository villageRepository, GovernorateRepository governorateRepository) {
        this.villageRepository = villageRepository;
        this.governorateRepository = governorateRepository;
    }

    public List<Village> getAllVillages() {
        return villageRepository.findAll();
    }

    public Village createVillage(CreateVillageRequest request) {
        Governorate governorate = governorateRepository.findById(request.governorateId())
                .orElseThrow(() -> new GovernorateNotFoundException(request.governorateId()));

        Village village = new Village(
                governorate,
                request.latitude(),
                request.longitude(),
                request.locationDescription(),
                request.elevationM()
        );

        return villageRepository.save(village);
    }
}