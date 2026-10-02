package com.qaryati.qaryati.population;

import com.qaryati.qaryati.user.User;
import com.qaryati.qaryati.user.UserRepository;
import com.qaryati.qaryati.village.Village;
import com.qaryati.qaryati.village.VillageRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PopulationRecordService {

    private final PopulationRecordRepository populationRecordRepository;
    private final VillageRepository villageRepository;
    private final UserRepository userRepository;

    public PopulationRecordService(PopulationRecordRepository populationRecordRepository,
                                   VillageRepository villageRepository,
                                   UserRepository userRepository) {
        this.populationRecordRepository = populationRecordRepository;
        this.villageRepository = villageRepository;
        this.userRepository = userRepository;
    }

    public List<PopulationRecord> getHistory(Long villageId) {
        if (!villageRepository.existsById(villageId)) {
            throw new VillageNotFoundException(villageId);
        }
        return populationRecordRepository.findByVillageIdOrderByYearAsc(villageId);
    }

    public PopulationRecord createRecord(Long villageId, CreatePopulationRecordRequest request) {
        Village village = villageRepository.findById(villageId)
                .orElseThrow(() -> new VillageNotFoundException(villageId));

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + currentUsername));

        PopulationRecord record = new PopulationRecord(
                village,
                request.year(),
                request.population(),
                request.source(),
                request.sourceUrl(),
                request.notes(),
                currentUser
        );

        return populationRecordRepository.save(record);
    }
}