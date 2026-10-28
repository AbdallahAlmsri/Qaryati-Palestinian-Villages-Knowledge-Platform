package com.qaryati.qaryati.economy;

import java.time.LocalDateTime;

public record VillageFacilityResponse(
        Long id,
        Long villageId,
        String facilityName,
        String facilityType,
        String description,
        String source,
        String status,
        Long createdBy,
        LocalDateTime createdAt,
        Long reviewedBy,
        LocalDateTime reviewedAt
) {
    public static VillageFacilityResponse from(VillageFacility facility) {
        return new VillageFacilityResponse(
                facility.getId(),
                facility.getVillage().getId(),
                facility.getFacilityName(),
                facility.getFacilityType(),
                facility.getDescription(),
                facility.getSource(),
                facility.getStatus(),
                facility.getCreatedBy() != null
                        ? facility.getCreatedBy().getId() : null,
                facility.getCreatedAt(),
                facility.getReviewedBy() != null
                        ? facility.getReviewedBy().getId() : null,
                facility.getReviewedAt()
        );
    }
}
