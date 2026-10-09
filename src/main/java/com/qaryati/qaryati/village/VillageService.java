package com.qaryati.qaryati.village;

import com.qaryati.qaryati.common.InvalidQueryException;
import com.qaryati.qaryati.governorate.Governorate;
import com.qaryati.qaryati.governorate.GovernorateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.TreeSet;

@Service
public class VillageService {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "locationDescription", "elevationM", "latitude", "longitude");
    private static final int MAX_PAGE_SIZE = 100;

    private final VillageRepository villageRepository;
    private final GovernorateRepository governorateRepository;

    public VillageService(VillageRepository villageRepository, GovernorateRepository governorateRepository) {
        this.villageRepository = villageRepository;
        this.governorateRepository = governorateRepository;
    }

    public Page<Village> search(Long governorateId, String q, int page, int size, String sort) {
        if (page < 0) {
            throw new InvalidQueryException("page must be 0 or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidQueryException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        Sort sorting = parseSort(sort);

        Specification<Village> spec = (root, query, cb) -> cb.conjunction();

        if (governorateId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("governorate").get("id"), governorateId));
        }

        if (q != null && !q.isBlank()) {
            String pattern = "%" + escapeLike(q.trim().toLowerCase()) + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.<String>get("locationDescription")), pattern, '\\'));
        }

        return villageRepository.findAll(spec, PageRequest.of(page, size, sorting));
    }

    public Village createVillage(CreateVillageRequest request) {
        Governorate governorate = governorateRepository.findById(request.governorateId())
                .orElseThrow(() -> new GovernorateNotFoundException(request.governorateId()));

        Village village = new Village(
                governorate,
                request.latitude(),
                request.longitude(),
                request.locationDescription(),
                request.elevationM()
        );

        return villageRepository.save(village);
    }

    private Sort parseSort(String sort) {
        String[] parts = sort.split(",");
        String field = parts[0].trim();

        if (!SORTABLE_FIELDS.contains(field)) {
            throw new InvalidQueryException(
                    "Cannot sort by '" + field + "'. Allowed fields: " + String.join(", ", new TreeSet<>(SORTABLE_FIELDS)));
        }

        Sort.Direction direction = Sort.Direction.ASC;
        if (parts.length > 1) {
            String requested = parts[1].trim().toLowerCase();
            if (requested.equals("desc")) {
                direction = Sort.Direction.DESC;
            } else if (!requested.equals("asc")) {
                throw new InvalidQueryException("Sort direction must be 'asc' or 'desc'");
            }
        }

        Sort sorting = Sort.by(direction, field);
        // id as a tie-breaker keeps page boundaries stable when many rows share a value
        return field.equals("id") ? sorting : sorting.and(Sort.by(Sort.Direction.ASC, "id"));
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}