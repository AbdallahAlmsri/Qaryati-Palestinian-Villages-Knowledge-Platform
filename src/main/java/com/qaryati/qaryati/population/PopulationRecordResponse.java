package com.qaryati.qaryati.population;

import java.time.LocalDateTime;

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
        LocalDateTime createdAt
) {
    public static PopulationRecordResponse from(PopulationRecord record) {
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
                record.getCreatedAt()
        );
    }
}