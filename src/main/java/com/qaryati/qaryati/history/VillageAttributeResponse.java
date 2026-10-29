package com.qaryati.qaryati.history;

import java.time.LocalDateTime;
import org.springframework.security.core.context.SecurityContextHolder;

public record VillageAttributeResponse(
        Long id,
        Long villageId,
        String attributeName,
        String attributeValue,
        String source,
        String status,
        String createdByUsername,
        LocalDateTime createdAt,
        String reviewedByUsername,
        LocalDateTime reviewedAt,
        String reviewNotes
) {
    public static VillageAttributeResponse from(VillageAttribute attribute) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean canReview = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_VERIFIER") || a.getAuthority().equals("ROLE_ADMIN"));
        return new VillageAttributeResponse(
                attribute.getId(),
                attribute.getVillage().getId(),
                attribute.getAttributeName(),
                attribute.getAttributeValue(),
                attribute.getSource(),
                attribute.getStatus(),
                attribute.getCreatedBy() == null ? null : attribute.getCreatedBy().getUsername(),
                attribute.getCreatedAt(),
                canReview && attribute.getReviewedBy() != null ? attribute.getReviewedBy().getUsername() : null,
                attribute.getReviewedAt(),
                canReview ? attribute.getReviewNotes() : null
        );
    }
}