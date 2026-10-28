package com.qaryati.qaryati.economy;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VillageEconomyRepository extends JpaRepository<VillageEconomy, Long> {

    List<VillageEconomy> findByVillageId(Long villageId);
}
