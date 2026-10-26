package com.qaryati.qaryati.economy;

import java.time.LocalDateTime;

public record VillageAgricultureResponse(
        Long id,
        Long villageId,
        String cropName,
        String description,
        String source,
        String status,
        Long createdBy,
        LocalDateTime createdAt,
        Long reviewedBy,
        LocalDateTime reviewedAt
) {
    public static VillageAgricultureResponse from(VillageAgriculture agriculture) {
        return new VillageAgricultureResponse(
                agriculture.getId(),
                agriculture.getVillage().getId(),
                agriculture.getCropName(),
                agriculture.getDescription(),
                agriculture.getSource(),
                agriculture.getStatus(),
                agriculture.getCreatedBy() != null
                        ? agriculture.getCreatedBy().getId() : null,
                agriculture.getCreatedAt(),
                agriculture.getReviewedBy() != null
                        ? agriculture.getReviewedBy().getId() : null,
                agriculture.getReviewedAt()
        );
    }
}