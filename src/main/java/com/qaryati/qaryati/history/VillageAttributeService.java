package com.qaryati.qaryati.history;

import com.qaryati.qaryati.user.User;
import com.qaryati.qaryati.user.UserRepository;
import com.qaryati.qaryati.village.Village;
import com.qaryati.qaryati.village.VillageNotFoundException;
import com.qaryati.qaryati.village.VillageRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VillageAttributeService {

    private final VillageAttributeRepository villageAttributeRepository;
    private final VillageRepository villageRepository;
    private final UserRepository userRepository;

    public VillageAttributeService(VillageAttributeRepository villageAttributeRepository,
                                   VillageRepository villageRepository,
                                   UserRepository userRepository) {
        this.villageAttributeRepository = villageAttributeRepository;
        this.villageRepository = villageRepository;
        this.userRepository = userRepository;
    }

    public List<VillageAttribute> getAttributes(Long villageId) {
        if (!villageRepository.existsById(villageId)) {
            throw new VillageNotFoundException(villageId);
        }

        return villageAttributeRepository.findByVillageId(villageId);
    }

    public VillageAttribute createAttribute(Long villageId, CreateVillageAttributeRequest request) {
        Village village = villageRepository.findById(villageId)
                .orElseThrow(() -> new VillageNotFoundException(villageId));

        String currentUsername = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found: " + currentUsername));

        VillageAttribute attribute = new VillageAttribute(
                village,
                request.attributeName(),
                request.attributeValue(),
                request.source(),
                currentUser
        );

        return villageAttributeRepository.save(attribute);
    }

    public VillageAttribute reviewAttribute(Long villageId, Long attributeId, HistoryReviewRequest request) {
        VillageAttribute attribute = villageAttributeRepository.findById(attributeId)
                .orElseThrow(() -> new InvalidHistoryReviewException(
                        "Village attribute with id " + attributeId + " does not exist"));

        if (!attribute.getVillage().getId().equals(villageId)) {
            throw new InvalidHistoryReviewException(
                    "Attribute " + attributeId + " does not belong to village " + villageId);
        }

        if (!"PENDING".equals(attribute.getStatus())) {
            throw new InvalidHistoryReviewException(
                    "Only PENDING attributes can be reviewed");
        }

        if (request.status() == HistoryStatus.PENDING) {
            throw new InvalidHistoryReviewException(
                    "Cannot set review status to PENDING");
        }

        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        User reviewer = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found: " + username));

        attribute.review(request.status().name(), reviewer);

        return villageAttributeRepository.save(attribute);
    }
}