package com.qaryati.qaryati.history;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record HistoricalEventResponse(
        Long id,
        Long villageId,
        String title,
        String description,
        LocalDate eventDate,
        String source,
        String status,
        String createdByUsername,
        LocalDateTime createdAt
) {
    public static HistoricalEventResponse from(HistoricalEvent event) {
        return new HistoricalEventResponse(
                event.getId(),
                event.getVillage().getId(),
                event.getTitle(),
                event.getDescription(),
                event.getEventDate(),
                event.getSource(),
                event.getStatus(),
                event.getCreatedBy() == null ? null : event.getCreatedBy().getUsername(),
                event.getCreatedAt()
        );
    }
}