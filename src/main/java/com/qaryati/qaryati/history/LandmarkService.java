package com.qaryati.qaryati.history;

import com.qaryati.qaryati.audit.AuditLogService;
import com.qaryati.qaryati.user.User;
import com.qaryati.qaryati.user.UserRepository;
import com.qaryati.qaryati.village.Village;
import com.qaryati.qaryati.village.VillageNotFoundException;
import com.qaryati.qaryati.village.VillageRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LandmarkService {

    private final LandmarkRepository landmarkRepository;
    private final VillageRepository villageRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public LandmarkService(LandmarkRepository landmarkRepository,
                           VillageRepository villageRepository,
                           UserRepository userRepository,
                           AuditLogService auditLogService) {
        this.landmarkRepository = landmarkRepository;
        this.villageRepository = villageRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    public List<Landmark> getLandmarks(Long villageId) {
        if (!villageRepository.existsById(villageId)) {
            throw new VillageNotFoundException(villageId);
        }

        return landmarkRepository.findByVillageId(villageId);
    }

    public Landmark createLandmark(Long villageId, CreateLandmarkRequest request) {
        Village village = villageRepository.findById(villageId)
                .orElseThrow(() -> new VillageNotFoundException(villageId));

        String currentUsername = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found: " + currentUsername));

        Landmark landmark = new Landmark(
                village,
                request.name(),
                request.description(),
                request.landmarkType(),
                request.latitude(),
                request.longitude(),
                request.source(),
                currentUser
        );

        return landmarkRepository.save(landmark);
    }

    @Transactional
    public Landmark reviewLandmark(Long villageId, Long landmarkId, HistoryReviewRequest request) {
        Landmark landmark = landmarkRepository.findById(landmarkId)
                .orElseThrow(() -> new InvalidHistoryReviewException(
                        "Landmark with id " + landmarkId + " does not exist"));

        if (!landmark.getVillage().getId().equals(villageId)) {
            throw new InvalidHistoryReviewException(
                    "Landmark " + landmarkId + " does not belong to village " + villageId);
        }

        if (!"PENDING".equals(landmark.getStatus())) {
            throw new InvalidHistoryReviewException(
                    "Only PENDING landmarks can be reviewed");
        }

        if (request.status() == HistoryStatus.PENDING) {
            throw new InvalidHistoryReviewException(
                    "Cannot set review status to PENDING");
        }

        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        User reviewer = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found: " + username));

        String oldStatus = landmark.getStatus();

        landmark.review(
                request.status().name(),
                reviewer,
                request.reviewNotes()
        );

        Landmark saved = landmarkRepository.save(landmark);

        auditLogService.record(
                "REVIEW",
                "LANDMARK",
                saved.getId(),
                oldStatus,
                saved.getStatus()
        );

        return saved;
    }
}
