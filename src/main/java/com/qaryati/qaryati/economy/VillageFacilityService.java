package com.qaryati.qaryati.economy;

import com.qaryati.qaryati.history.InvalidHistoryReviewException;
import com.qaryati.qaryati.user.User;
import com.qaryati.qaryati.user.UserRepository;
import com.qaryati.qaryati.village.Village;
import com.qaryati.qaryati.village.VillageNotFoundException;
import com.qaryati.qaryati.village.VillageRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VillageFacilityService {

    private final VillageFacilityRepository facilityRepository;
    private final VillageRepository villageRepository;
    private final UserRepository userRepository;

    public VillageFacilityService(
            VillageFacilityRepository facilityRepository,
            VillageRepository villageRepository,
            UserRepository userRepository) {
        this.facilityRepository = facilityRepository;
        this.villageRepository = villageRepository;
        this.userRepository = userRepository;
    }

    public List<VillageFacility> getFacilities(Long villageId) {
        if (!villageRepository.existsById(villageId)) {
            throw new VillageNotFoundException(villageId);
        }

        return facilityRepository.findByVillageId(villageId);
    }

    public VillageFacility createFacility(
            Long villageId,
            String facilityName,
            String facilityType,
            String description,
            String source) {

        Village village = villageRepository.findById(villageId)
                .orElseThrow(() -> new VillageNotFoundException(villageId));

        User currentUser = getCurrentUser();

        VillageFacility facility = new VillageFacility(
                village,
                facilityName,
                facilityType,
                description,
                source,
                currentUser
        );

        return facilityRepository.save(facility);
    }

    public VillageFacility reviewFacility(
            Long villageId, Long facilityId, String status) {

        VillageFacility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new InvalidHistoryReviewException(
                        "Facility record not found: " + facilityId));

        if (!facility.getVillage().getId().equals(villageId)) {
            throw new InvalidHistoryReviewException(
                    "Facility record does not belong to this village");
        }

        if (!"PENDING".equals(facility.getStatus())) {
            throw new InvalidHistoryReviewException(
                    "Only PENDING records can be reviewed");
        }

        if (!"VERIFIED".equals(status) && !"REJECTED".equals(status)) {
            throw new InvalidHistoryReviewException(
                    "Invalid facility review status");
        }

        facility.review(status, getCurrentUser());

        return facilityRepository.save(facility);
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found: " + username));
    }
}
