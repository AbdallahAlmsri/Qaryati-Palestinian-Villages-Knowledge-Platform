package com.qaryati.qaryati.governorate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MunicipalityRepository extends JpaRepository<Municipality, Long> {

    List<Municipality> findByGovernorateIdOrderByNameAsc(Long governorateId);
}
