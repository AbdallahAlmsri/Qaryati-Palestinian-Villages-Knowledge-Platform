package com.qaryati.qaryati.history;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateLandmarkRequest(
        @NotBlank @Size(max = 200) String name,
        String description,
        @Size(max = 100) String landmarkType,
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        @NotBlank String source
) {
}