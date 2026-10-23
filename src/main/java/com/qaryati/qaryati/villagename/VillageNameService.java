package com.qaryati.qaryati.villagename;

import com.qaryati.qaryati.user.User;
import com.qaryati.qaryati.user.UserRepository;
import com.qaryati.qaryati.village.Village;
import com.qaryati.qaryati.village.VillageNotFoundException;
import com.qaryati.qaryati.village.VillageRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VillageNameService {

    private final VillageNameRepository villageNameRepository;
    private final VillageRepository villageRepository;
    private final UserRepository userRepository;

    public VillageNameService(VillageNameRepository villageNameRepository,
                              VillageRepository villageRepository,
                              UserRepository userRepository) {
        this.villageNameRepository = villageNameRepository;
        this.villageRepository = villageRepository;
        this.userRepository = userRepository;
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
}