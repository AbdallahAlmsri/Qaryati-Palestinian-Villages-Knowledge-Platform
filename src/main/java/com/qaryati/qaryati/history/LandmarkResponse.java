package com.qaryati.qaryati.history;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.security.core.context.SecurityContextHolder;

public record LandmarkResponse(
        Long id,
        Long villageId,
        String name,
        String description,
        String landmarkType,
        BigDecimal latitude,
        BigDecimal longitude,
        String source,
        String status,
        String createdByUsername,
        LocalDateTime createdAt,
        String reviewedByUsername,
        LocalDateTime reviewedAt,
        String reviewNotes
) {
    public static LandmarkResponse from(Landmark landmark) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean canReview = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_VERIFIER") || a.getAuthority().equals("ROLE_ADMIN"));
        return new LandmarkResponse(
                landmark.getId(),
                landmark.getVillage().getId(),
                landmark.getName(),
                landmark.getDescription(),
                landmark.getLandmarkType(),
                landmark.getLatitude(),
                landmark.getLongitude(),
                landmark.getSource(),
                landmark.getStatus(),
                landmark.getCreatedBy() == null ? null : landmark.getCreatedBy().getUsername(),
                landmark.getCreatedAt(),
                canReview && landmark.getReviewedBy() != null ? landmark.getReviewedBy().getUsername() : null,
                landmark.getReviewedAt(),
                canReview ? landmark.getReviewNotes() : null
        );
    }
}