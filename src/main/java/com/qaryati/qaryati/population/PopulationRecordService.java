package com.qaryati.qaryati.population;

import com.qaryati.qaryati.user.User;
import com.qaryati.qaryati.user.UserRepository;
import com.qaryati.qaryati.village.Village;
import com.qaryati.qaryati.village.VillageNotFoundException;
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
    public PopulationRecord reviewRecord(Long villageId, Long recordId, ReviewDecisionRequest request) {
        PopulationRecord record = populationRecordRepository.findById(recordId)
                .orElseThrow(() -> new InvalidReviewException("Population record with id " + recordId + " does not exist"));

        if (!java.util.Objects.equals(record.getVillage().getId(), villageId)) {
            throw new InvalidReviewException("Record " + recordId + " does not belong to village " + villageId);
        }

        if (record.getStatus() != PopulationStatus.PENDING) {
            throw new InvalidReviewException("Only PENDING records can be reviewed; this record is already " + record.getStatus());
        }

        if (request.status() == PopulationStatus.PENDING) {
            throw new InvalidReviewException("Cannot set review status back to PENDING");
        }

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User reviewer = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + currentUsername));

        record.review(request.status(), reviewer);
        return populationRecordRepository.save(record);
    }

    public PopulationStatsResponse getStats(Long villageId, Integer fromYear, Integer toYear) {
        if (!villageRepository.existsById(villageId)) {
            throw new VillageNotFoundException(villageId);
        }

        if (fromYear > toYear) {
            throw new InvalidReviewException("fromYear must not be after toYear");
        }

        List<Object[]> rows = populationRecordRepository.getPopulationStats(villageId, fromYear, toYear);
        Object[] row = rows.isEmpty() ? new Object[]{null, null, null, 0L} : rows.get(0);

        return new PopulationStatsResponse(
                villageId,
                fromYear,
                toYear,
                row[0] != null ? ((Number) row[0]).intValue() : null,
                row[1] != null ? ((Number) row[1]).intValue() : null,
                row[2] != null ? ((Number) row[2]).intValue() : null,
                row[3] != null ? ((Number) row[3]).longValue() : 0L
        );
    }
}