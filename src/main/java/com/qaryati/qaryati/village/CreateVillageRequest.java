package com.qaryati.qaryati.village;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record CreateVillageRequest(
        @NotNull Long governorateId,
        @DecimalMin("-90.0") @DecimalMax("90.0") java.math.BigDecimal latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") java.math.BigDecimal longitude,
        String locationDescription,
        Integer elevationM
) {
}