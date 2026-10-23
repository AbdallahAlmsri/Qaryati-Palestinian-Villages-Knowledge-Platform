package com.qaryati.qaryati.villagename;

import java.time.LocalDateTime;

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
        LocalDateTime createdAt
) {
    public static VillageNameResponse from(VillageName villageName) {
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
                villageName.getCreatedAt()
        );
    }
}