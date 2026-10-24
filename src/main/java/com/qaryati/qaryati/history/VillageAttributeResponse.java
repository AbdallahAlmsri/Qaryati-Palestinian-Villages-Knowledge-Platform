package com.qaryati.qaryati.history;

import java.time.LocalDateTime;

public record VillageAttributeResponse(
        Long id,
        Long villageId,
        String attributeName,
        String attributeValue,
        String source,
        String status,
        String createdByUsername,
        LocalDateTime createdAt
) {
    public static VillageAttributeResponse from(VillageAttribute attribute) {
        return new VillageAttributeResponse(
                attribute.getId(),
                attribute.getVillage().getId(),
                attribute.getAttributeName(),
                attribute.getAttributeValue(),
                attribute.getSource(),
                attribute.getStatus(),
                attribute.getCreatedBy() == null ? null : attribute.getCreatedBy().getUsername(),
                attribute.getCreatedAt()
        );
    }
}