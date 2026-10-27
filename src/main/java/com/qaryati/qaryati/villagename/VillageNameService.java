package com.qaryati.qaryati.villagename;

import com.qaryati.qaryati.audit.AuditLogService;
import com.qaryati.qaryati.user.User;
import com.qaryati.qaryati.user.UserRepository;
import com.qaryati.qaryati.village.Village;
import com.qaryati.qaryati.village.VillageNotFoundException;
import com.qaryati.qaryati.village.VillageRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VillageNameService {

    private final VillageNameRepository villageNameRepository;
    private final VillageRepository villageRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public VillageNameService(VillageNameRepository villageNameRepository,
                              VillageRepository villageRepository,
                              UserRepository userRepository,
                              AuditLogService auditLogService) {
        this.villageNameRepository = villageNameRepository;
        this.villageRepository = villageRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    public List<VillageNameResponse> getNames(Long villageId) {
        if (!villageRepository.existsById(villageId)) {
            throw new VillageNotFoundException(villageId);
        }

        return villageNameRepository.findByVillageId(villageId)
                .stream()
                .map(VillageNameResponse::from)
                .toList();
    }

    public VillageNameResponse createName(Long villageId, VillageNameRequest request) {
        Village village = villageRepository.findById(villageId)
                .orElseThrow(() -> new VillageNotFoundException(villageId));

        String currentUsername = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found: " + currentUsername));

        VillageName villageName = new VillageName(
                village,
                request.name(),
                request.nameType(),
                request.language(),
                request.validFrom(),
                request.validTo(),
                request.source(),
                request.nameOrigin(),
                currentUser
        );

        VillageName savedName = villageNameRepository.save(villageName);

        return VillageNameResponse.from(savedName);
    }

    @Transactional
    public VillageNameResponse reviewName(
            Long villageId,
            Long nameId,
            VillageNameReviewRequest request) {

        VillageName villageName = villageNameRepository.findById(nameId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Village name with id " + nameId + " does not exist"));

        if (!villageName.getVillage().getId().equals(villageId)) {
            throw new IllegalArgumentException(
                    "Village name does not belong to village " + villageId);
        }

        if (!"PENDING".equals(villageName.getStatus())) {
            throw new IllegalStateException(
                    "Only PENDING village names can be reviewed");
        }

        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User reviewer = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found: " + username));

        String oldStatus = villageName.getStatus();

        villageName.review(
                request.status(),
                reviewer,
                request.reviewNotes()
        );

        VillageName saved = villageNameRepository.save(villageName);

        auditLogService.record(
                "REVIEW",
                "VILLAGE_NAME",
                saved.getId(),
                oldStatus,
                saved.getStatus()
        );

        return VillageNameResponse.from(saved);
    }
}
