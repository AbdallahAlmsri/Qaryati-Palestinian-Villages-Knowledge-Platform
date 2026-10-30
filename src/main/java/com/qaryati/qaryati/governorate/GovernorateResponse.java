package com.qaryati.qaryati.governorate;

public record GovernorateResponse(
        Long id,
        String name
) {
    public static GovernorateResponse from(Governorate governorate) {
        return new GovernorateResponse(governorate.getId(), governorate.getName());
    }
}
