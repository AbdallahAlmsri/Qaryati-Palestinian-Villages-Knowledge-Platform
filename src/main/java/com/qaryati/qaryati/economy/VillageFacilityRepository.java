package com.qaryati.qaryati.economy;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VillageFacilityRepository extends JpaRepository<VillageFacility, Long> {

    List<VillageFacility> findByVillageId(Long villageId);
}
