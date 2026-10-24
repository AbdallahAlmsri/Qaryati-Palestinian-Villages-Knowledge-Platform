package com.qaryati.qaryati.villagename;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VillageNameRequest(

        @NotBlank
        @Size(max = 200)
        String name,

        @NotBlank
        @Size(max = 30)
        String nameType,

        @Size(max = 20)
        String language,

        Integer validFrom,

        Integer validTo,

        String source,

        String nameOrigin
) {
}