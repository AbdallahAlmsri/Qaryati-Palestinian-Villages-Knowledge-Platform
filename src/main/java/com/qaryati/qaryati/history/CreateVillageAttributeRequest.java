package com.qaryati.qaryati.history;

import jakarta.validation.constraints.*;

public record CreateVillageAttributeRequest(
        @NotBlank @Size(max = 150) String attributeName,
        @NotBlank String attributeValue,
        @NotBlank String source
) {
}