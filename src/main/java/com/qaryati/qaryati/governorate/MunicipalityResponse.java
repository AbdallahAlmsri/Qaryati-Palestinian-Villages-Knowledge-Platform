package com.qaryati.qaryati.governorate;

public record MunicipalityResponse(
        Long id,
        Long governorateId,
        String name
) {
    public static MunicipalityResponse from(Municipality municipality) {
        return new MunicipalityResponse(
                municipality.getId(),
                municipality.getGovernorate().getId(),
                municipality.getName()
        );
    }
}
