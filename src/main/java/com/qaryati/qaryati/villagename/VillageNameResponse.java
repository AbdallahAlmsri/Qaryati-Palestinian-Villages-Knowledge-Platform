package com.qaryati.qaryati.villagename;

import java.time.LocalDateTime;
import org.springframework.security.core.context.SecurityContextHolder;

public record VillageNameResponse(
        Long id,
        Long villageId,
        String name,
        String nameType,
        String language,
        Integer validFrom,
        Integer validTo,
        String source,
        String nameOrigin,
        String status,
        Long createdBy,
        LocalDateTime createdAt,
        Long reviewedBy,
        LocalDateTime reviewedAt,
        String reviewNotes
) {
    public static VillageNameResponse from(VillageName villageName) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean canViewReview = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_VERIFIER") || a.getAuthority().equals("ROLE_ADMIN"));
        return new VillageNameResponse(
                villageName.getId(),
                villageName.getVillage().getId(),
                villageName.getName(),
                villageName.getNameType(),
                villageName.getLanguage(),
                villageName.getValidFrom(),
                villageName.getValidTo(),
                villageName.getSource(),
                villageName.getNameOrigin(),
                villageName.getStatus(),
                villageName.getCreatedBy() != null
                        ? villageName.getCreatedBy().getId()
                        : null,
                villageName.getCreatedAt(),
                canViewReview && villageName.getReviewedBy() != null
                        ? villageName.getReviewedBy().getId()
                        : null,
                villageName.getReviewedAt(),
                canViewReview ? villageName.getReviewNotes() : null
        );
    }
}
