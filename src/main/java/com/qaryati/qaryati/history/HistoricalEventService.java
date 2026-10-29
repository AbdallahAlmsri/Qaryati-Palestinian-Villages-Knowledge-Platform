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
public class HistoricalEventService {

    private final HistoricalEventRepository historicalEventRepository;
    private final VillageRepository villageRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public HistoricalEventService(HistoricalEventRepository historicalEventRepository,
                                  VillageRepository villageRepository,
                                  UserRepository userRepository,
                                  AuditLogService auditLogService) {
        this.historicalEventRepository = historicalEventRepository;
        this.villageRepository = villageRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    public List<HistoricalEvent> getEvents(Long villageId) {
        if (!villageRepository.existsById(villageId)) {
            throw new VillageNotFoundException(villageId);
        }

        return historicalEventRepository.findByVillageId(villageId);
    }

    public HistoricalEvent createEvent(Long villageId, CreateHistoricalEventRequest request) {
        Village village = villageRepository.findById(villageId)
                .orElseThrow(() -> new VillageNotFoundException(villageId));

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found: " + currentUsername));

        HistoricalEvent event = new HistoricalEvent(
                village,
                request.title(),
                request.description(),
                request.eventDate(),
                request.source(),
                currentUser
        );

        return historicalEventRepository.save(event);
    }

    @Transactional
    public HistoricalEvent reviewEvent(Long villageId, Long eventId, HistoryReviewRequest request) {
        HistoricalEvent event = historicalEventRepository.findById(eventId)
                .orElseThrow(() -> new InvalidHistoryReviewException(
                        "Historical event with id " + eventId + " does not exist"));

        if (!event.getVillage().getId().equals(villageId)) {
            throw new InvalidHistoryReviewException(
                    "Event " + eventId + " does not belong to village " + villageId);
        }

        if (!"PENDING".equals(event.getStatus())) {
            throw new InvalidHistoryReviewException(
                    "Only PENDING events can be reviewed");
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

        String oldStatus = event.getStatus();

        event.review(
                request.status().name(),
                reviewer,
                request.reviewNotes()
        );

        HistoricalEvent saved = historicalEventRepository.save(event);

        auditLogService.record(
                "REVIEW",
                "HISTORICAL_EVENT",
                saved.getId(),
                oldStatus,
                saved.getStatus()
        );

        return saved;
    }
}
