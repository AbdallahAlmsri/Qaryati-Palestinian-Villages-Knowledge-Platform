package com.qaryati.qaryati.economy;

import com.qaryati.qaryati.common.InvalidReviewException;
import com.qaryati.qaryati.user.User;
import com.qaryati.qaryati.user.UserRepository;
import com.qaryati.qaryati.village.Village;
import com.qaryati.qaryati.village.VillageNotFoundException;
import com.qaryati.qaryati.village.VillageRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VillageAgricultureService {

    private final VillageAgricultureRepository agricultureRepository;
    private final VillageRepository villageRepository;
    private final UserRepository userRepository;

    public VillageAgricultureService(
            VillageAgricultureRepository agricultureRepository,
            VillageRepository villageRepository,
            UserRepository userRepository) {
        this.agricultureRepository = agricultureRepository;
        this.villageRepository = villageRepository;
        this.userRepository = userRepository;
    }

    public List<VillageAgriculture> getAgriculture(Long villageId) {
        if (!villageRepository.existsById(villageId)) {
            throw new VillageNotFoundException(villageId);
        }

        return agricultureRepository.findByVillageId(villageId);
    }

    public VillageAgriculture createAgriculture(
            Long villageId, String cropName, String description, String source) {

        Village village = villageRepository.findById(villageId)
                .orElseThrow(() -> new VillageNotFoundException(villageId));

        User currentUser = getCurrentUser();

        VillageAgriculture agriculture = new VillageAgriculture(
                village, cropName, description, source, currentUser);

        return agricultureRepository.save(agriculture);
    }

    public VillageAgriculture reviewAgriculture(
            Long villageId, Long agricultureId, String status) {

        VillageAgriculture agriculture = agricultureRepository.findById(agricultureId)
                .orElseThrow(() -> new InvalidReviewException(
                        "Agriculture record not found: " + agricultureId));

        if (!agriculture.getVillage().getId().equals(villageId)) {
            throw new InvalidReviewException(
                    "Agriculture record does not belong to this village");
        }

        if (!"PENDING".equals(agriculture.getStatus())) {
            throw new InvalidReviewException(
                    "Only PENDING records can be reviewed");
        }

        if (!"VERIFIED".equals(status) && !"REJECTED".equals(status)) {
            throw new InvalidReviewException(
                    "Invalid agriculture review status");
        }

        agriculture.review(status, getCurrentUser());

        return agricultureRepository.save(agriculture);
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found: " + username));
    }
}