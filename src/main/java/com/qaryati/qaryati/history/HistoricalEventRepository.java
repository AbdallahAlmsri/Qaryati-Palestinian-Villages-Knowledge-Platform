package com.qaryati.qaryati.history;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistoricalEventRepository extends JpaRepository<HistoricalEvent, Long> {
    List<HistoricalEvent> findByVillageId(Long villageId);
}