package com.qaryati.qaryati.governorate;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GovernorateService {

    private final GovernorateRepository governorateRepository;
    private final MunicipalityRepository municipalityRepository;

    public GovernorateService(GovernorateRepository governorateRepository,
                              MunicipalityRepository municipalityRepository) {
        this.governorateRepository = governorateRepository;
        this.municipalityRepository = municipalityRepository;
    }

    public List<Governorate> getGovernorates() {
        return governorateRepository.findAll(Sort.by("name"));
    }

    public List<Municipality> getMunicipalities(Long governorateId) {
        if (!governorateRepository.existsById(governorateId)) {
            throw new GovernorateNotFoundException(governorateId);
        }

        return municipalityRepository.findByGovernorateIdOrderByNameAsc(governorateId);
    }
}
