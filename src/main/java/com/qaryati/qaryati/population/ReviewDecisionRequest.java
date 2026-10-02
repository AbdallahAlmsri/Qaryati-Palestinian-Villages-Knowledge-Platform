package com.qaryati.qaryati.population;

import jakarta.validation.constraints.NotNull;

public record ReviewDecisionRequest(
        @NotNull PopulationStatus status,
        String reviewNotes
) {
}