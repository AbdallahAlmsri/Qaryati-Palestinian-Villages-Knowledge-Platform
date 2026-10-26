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
public class VillageEconomyService {

    private final VillageEconomyRepository economyRepository;
    private final VillageRepository villageRepository;
    private final UserRepository userRepository;

    public VillageEconomyService(
            VillageEconomyRepository economyRepository,
            VillageRepository villageRepository,
            UserRepository userRepository) {
        this.economyRepository = economyRepository;
        this.villageRepository = villageRepository;
        this.userRepository = userRepository;
    }

    public List<VillageEconomy> getEconomy(Long villageId) {
        if (!villageRepository.existsById(villageId)) {
            throw new VillageNotFoundException(villageId);
        }

        return economyRepository.findByVillageId(villageId);
    }

    public VillageEconomy createEconomy(
            Long villageId, String activityName, String description, String source) {

        Village village = villageRepository.findById(villageId)
                .orElseThrow(() -> new VillageNotFoundException(villageId));

        User currentUser = getCurrentUser();

        VillageEconomy economy = new VillageEconomy(
                village, activityName, description, source, currentUser);

        return economyRepository.save(economy);
    }

    public VillageEconomy reviewEconomy(
            Long villageId, Long economyId, String status) {

        VillageEconomy economy = economyRepository.findById(economyId)
                .orElseThrow(() -> new InvalidHistoryReviewException(
                        "Economy record not found: " + economyId));

        if (!economy.getVillage().getId().equals(villageId)) {
            throw new InvalidHistoryReviewException(
                    "Economy record does not belong to this village");
        }

        if (!"PENDING".equals(economy.getStatus())) {
            throw new InvalidHistoryReviewException(
                    "Only PENDING records can be reviewed");
        }

        if (!"VERIFIED".equals(status) && !"REJECTED".equals(status)) {
            throw new InvalidHistoryReviewException(
                    "Invalid economy review status");
        }

        economy.review(status, getCurrentUser());

        return economyRepository.save(economy);
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found: " + username));
    }
}