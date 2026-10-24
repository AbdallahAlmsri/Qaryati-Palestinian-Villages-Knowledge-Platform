package com.qaryati.qaryati.history;

import jakarta.validation.constraints.NotNull;

public record HistoryReviewRequest(
        @NotNull HistoryStatus status,
        String reviewNotes
) {
}