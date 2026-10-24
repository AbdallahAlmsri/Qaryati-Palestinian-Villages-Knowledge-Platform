package com.qaryati.qaryati.history;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
        LocalDateTime createdAt
) {
    public static LandmarkResponse from(Landmark landmark) {
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
                landmark.getCreatedAt()
        );
    }
}