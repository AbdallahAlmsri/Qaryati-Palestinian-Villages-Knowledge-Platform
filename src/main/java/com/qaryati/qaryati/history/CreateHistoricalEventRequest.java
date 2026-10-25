package com.qaryati.qaryati.history;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record CreateHistoricalEventRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String description,
        LocalDate eventDate,
        @NotBlank String source
) {
}