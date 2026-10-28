package com.qaryati.qaryati.population;

import java.time.LocalDateTime;
import org.springframework.security.core.context.SecurityContextHolder;

public record PopulationRecordResponse(
        Long id,
        Long villageId,
        Integer year,
        Integer population,
        String source,
        String sourceUrl,
        String notes,
        String status,
        String createdByUsername,
        LocalDateTime createdAt,
        String reviewedByUsername,
        LocalDateTime reviewedAt,
        String reviewNotes
) {
    public static PopulationRecordResponse from(PopulationRecord record) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean canViewReview = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_VERIFIER") || a.getAuthority().equals("ROLE_ADMIN"));
        return new PopulationRecordResponse(
                record.getId(),
                record.getVillage().getId(),
                record.getYear(),
                record.getPopulation(),
                record.getSource(),
                record.getSourceUrl(),
                record.getNotes(),
                record.getStatus().name(),
                record.getCreatedBy().getUsername(),
                record.getCreatedAt(),
                canViewReview && record.getReviewedBy() != null
                        ? record.getReviewedBy().getUsername() : null,
                record.getReviewedAt(),
                canViewReview ? record.getReviewNotes() : null
        );
    }
}