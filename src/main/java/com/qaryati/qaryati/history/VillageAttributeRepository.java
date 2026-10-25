package com.qaryati.qaryati.history;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VillageAttributeRepository extends JpaRepository<VillageAttribute, Long> {
    List<VillageAttribute> findByVillageId(Long villageId);
}