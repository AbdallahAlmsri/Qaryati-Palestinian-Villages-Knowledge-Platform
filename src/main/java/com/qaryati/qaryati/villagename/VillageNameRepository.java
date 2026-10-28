package com.qaryati.qaryati.villagename;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VillageNameRepository extends JpaRepository<VillageName, Long> {

    List<VillageName> findByVillageId(Long villageId);

    Optional<VillageName> findFirstByVillageIdAndLanguageIgnoreCaseAndStatusIgnoreCaseOrderByIdDesc(
            Long villageId, String language, String status
    );

    Optional<VillageName> findFirstByVillageIdAndLanguageIgnoreCaseOrderByIdDesc(
            Long villageId, String language
    );
}