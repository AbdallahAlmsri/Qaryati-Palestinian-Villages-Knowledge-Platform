package com.qaryati.qaryati.history;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.security.core.context.SecurityContextHolder;

public record HistoricalEventResponse(
        Long id,
        Long villageId,
        String title,
        String description,
        LocalDate eventDate,
        String source,
        String status,
        String createdByUsername,
        LocalDateTime createdAt,
        String reviewedByUsername,
        LocalDateTime reviewedAt,
        String reviewNotes
) {
    public static HistoricalEventResponse from(HistoricalEvent event) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean canReview = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_VERIFIER") || a.getAuthority().equals("ROLE_ADMIN"));
        return new HistoricalEventResponse(
                event.getId(),
                event.getVillage().getId(),
                event.getTitle(),
                event.getDescription(),
                event.getEventDate(),
                event.getSource(),
                event.getStatus(),
                event.getCreatedBy() == null ? null : event.getCreatedBy().getUsername(),
                event.getCreatedAt(),
                canReview && event.getReviewedBy() != null ? event.getReviewedBy().getUsername() : null,
                event.getReviewedAt(),
                canReview ? event.getReviewNotes() : null
        );
    }
}