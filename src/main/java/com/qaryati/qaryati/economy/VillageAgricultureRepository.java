package com.qaryati.qaryati.economy;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VillageAgricultureRepository extends JpaRepository<VillageAgriculture, Long> {

    List<VillageAgriculture> findByVillageId(Long villageId);
}