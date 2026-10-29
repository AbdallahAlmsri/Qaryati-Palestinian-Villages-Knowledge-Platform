package com.qaryati.qaryati.economy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateVillageAgricultureRequest(

        @NotBlank
        @Size(max = 150)
        String cropName,

        String description,

        @NotBlank
        String source

) {}
