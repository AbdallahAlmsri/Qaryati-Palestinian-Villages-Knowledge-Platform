package com.qaryati.qaryati.population;

public record PopulationStatsResponse(
        Long villageId,
        Integer fromYear,
        Integer toYear,
        Integer minPopulation,
        Integer maxPopulation,
        Integer avgPopulation,
        Long recordCount
) {
}