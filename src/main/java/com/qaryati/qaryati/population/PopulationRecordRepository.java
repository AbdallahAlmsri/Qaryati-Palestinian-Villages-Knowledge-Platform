package com.qaryati.qaryati.population;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PopulationRecordRepository extends JpaRepository<PopulationRecord, Long> {
    List<PopulationRecord> findByVillageIdOrderByYearAsc(Long villageId);
}