package com.qaryati.qaryati.villagename;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record VillageNameReviewRequest(
        @NotNull
        @Pattern(regexp = "VERIFIED|REJECTED",
                 message = "Status must be VERIFIED or REJECTED")
        String status,

        String reviewNotes
) {
}
