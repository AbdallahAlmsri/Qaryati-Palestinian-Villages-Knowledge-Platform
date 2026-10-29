package com.qaryati.qaryati.history;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LandmarkRepository extends JpaRepository<Landmark, Long> {
    List<Landmark> findByVillageId(Long villageId);
}