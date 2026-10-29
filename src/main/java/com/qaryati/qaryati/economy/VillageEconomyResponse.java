package com.qaryati.qaryati.economy;

import java.time.LocalDateTime;

public record VillageEconomyResponse(
        Long id,
        Long villageId,
        String activityName,
        String description,
        String source,
        String status,
        Long createdBy,
        LocalDateTime createdAt,
        Long reviewedBy,
        LocalDateTime reviewedAt
) {
    public static VillageEconomyResponse from(VillageEconomy economy) {
        return new VillageEconomyResponse(
                economy.getId(),
                economy.getVillage().getId(),
                economy.getActivityName(),
                economy.getDescription(),
                economy.getSource(),
                economy.getStatus(),
                economy.getCreatedBy() != null
                        ? economy.getCreatedBy().getId() : null,
                economy.getCreatedAt(),
                economy.getReviewedBy() != null
                        ? economy.getReviewedBy().getId() : null,
                economy.getReviewedAt()
        );
    }
}
