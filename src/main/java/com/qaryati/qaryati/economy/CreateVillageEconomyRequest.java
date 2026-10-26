package com.qaryati.qaryati.economy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateVillageEconomyRequest(

        @NotBlank
        @Size(max = 150)
        String activityName,

        String description,

        @NotBlank
        String source

) {}
