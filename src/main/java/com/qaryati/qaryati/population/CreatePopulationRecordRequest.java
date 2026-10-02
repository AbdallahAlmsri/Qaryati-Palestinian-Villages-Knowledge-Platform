package com.qaryati.qaryati.population;

import jakarta.validation.constraints.*;

public record CreatePopulationRecordRequest(
        @NotNull @Min(0) Integer population,
        @NotNull @Min(1800) @Max(2100) Integer year,
        @NotBlank @Size(max = 255) String source,
        @Size(max = 500) String sourceUrl,
        String notes
) {
}