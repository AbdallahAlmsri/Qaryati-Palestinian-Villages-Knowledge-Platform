package com.qaryati.qaryati.population;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PopulationRecordRepository extends JpaRepository<PopulationRecord, Long> {

    List<PopulationRecord> findByVillageIdOrderByYearAsc(Long villageId);

    @Query(value = """
            SELECT
                MIN(population) AS min_population,
                MAX(population) AS max_population,
                ROUND(AVG(population)) AS avg_population,
                COUNT(*) AS record_count
            FROM population_records
            WHERE village_id = :villageId
              AND year BETWEEN :fromYear AND :toYear
              AND status = 'VERIFIED'
            """, nativeQuery = true)

    List<Object[]> getPopulationStats(
            @Param("villageId") Long villageId,
            @Param("fromYear") Integer fromYear,
            @Param("toYear") Integer toYear
    );
}