package com.qaryati.qaryati.villagename;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VillageNameRepository extends JpaRepository<VillageName, Long> {

    List<VillageName> findByVillageId(Long villageId);

}