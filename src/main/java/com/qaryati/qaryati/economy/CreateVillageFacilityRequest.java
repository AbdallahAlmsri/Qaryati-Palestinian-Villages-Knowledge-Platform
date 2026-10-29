package com.qaryati.qaryati.economy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateVillageFacilityRequest(

        @NotBlank
        @Size(max = 150)
        String facilityName,

        @NotBlank
        @Size(max = 100)
        String facilityType,

        String description,

        @NotBlank
        String source

) {}
