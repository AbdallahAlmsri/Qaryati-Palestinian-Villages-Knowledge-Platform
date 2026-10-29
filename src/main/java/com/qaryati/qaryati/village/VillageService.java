package com.qaryati.qaryati.village;

import com.qaryati.qaryati.common.InvalidQueryException;
import com.qaryati.qaryati.population.PopulationRecordRepository;
import com.qaryati.qaryati.governorate.Governorate;
import com.qaryati.qaryati.governorate.GovernorateNotFoundException;
import com.qaryati.qaryati.governorate.GovernorateRepository;
import com.qaryati.qaryati.governorate.InvalidMunicipalityException;
import com.qaryati.qaryati.governorate.Municipality;
import com.qaryati.qaryati.governorate.MunicipalityRepository;
import com.qaryati.qaryati.villagename.VillageName;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Service
public class VillageService {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "locationDescription", "elevationM", "latitude", "longitude");

    private static final int MAX_PAGE_SIZE = 100;

    private static final double MAX_RADIUS_KM = 100;
    private static final int MAX_NEARBY_LIMIT = 100;

    // Kilometres per degree of latitude (and of longitude at the equator)
    private static final double KM_PER_DEGREE = 111.045;

    private final VillageRepository villageRepository;
    private final GovernorateRepository governorateRepository;
    private final MunicipalityRepository municipalityRepository;
    private final PopulationRecordRepository populationRecordRepository;

    public VillageService(VillageRepository villageRepository,
                          GovernorateRepository governorateRepository,
                          MunicipalityRepository municipalityRepository,
                          PopulationRecordRepository populationRecordRepository) {
        this.villageRepository = villageRepository;
        this.governorateRepository = governorateRepository;
        this.municipalityRepository = municipalityRepository;
        this.populationRecordRepository = populationRecordRepository;
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

            spec = spec.and((root, query, cb) -> {
                Subquery<Long> nameSubquery = query.subquery(Long.class);

                var villageName = nameSubquery.from(VillageName.class);

                nameSubquery.select(villageName.get("village").get("id"))
                        .where(
                                cb.equal(
                                        villageName.get("village").get("id"),
                                        root.get("id")
                                ),
                                cb.like(
                                        cb.lower(villageName.<String>get("name")),
                                        pattern,
                                        '\\'
                                )
                        );

                return cb.or(
                        cb.like(
                                cb.lower(root.<String>get("locationDescription")),
                                pattern,
                                '\\'
                        ),
                        cb.exists(nameSubquery)
                );
            });
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

        if (request.municipalityId() != null) {
            village.setMunicipality(findMunicipalityIn(governorate, request.municipalityId()));
        }

        return villageRepository.save(village);
    }

    public List<NearbyVillageRow> findNearby(Long villageId, double radiusKm, int limit) {
        // NaN fails every comparison, so it would slip past the range check on its own
        if (Double.isNaN(radiusKm) || radiusKm <= 0 || radiusKm > MAX_RADIUS_KM) {
            throw new InvalidQueryException("radiusKm must be greater than 0 and at most " + (int) MAX_RADIUS_KM);
        }

        if (limit < 1 || limit > MAX_NEARBY_LIMIT) {
            throw new InvalidQueryException("limit must be between 1 and " + MAX_NEARBY_LIMIT);
        }

        Village village = villageRepository.findById(villageId)
                .orElseThrow(() -> new VillageNotFoundException(villageId));

        if (village.getLatitude() == null || village.getLongitude() == null) {
            throw new InvalidQueryException("Village " + villageId + " has no coordinates, so nearby villages cannot be found");
        }

        double latitude = village.getLatitude().doubleValue();
        double longitude = village.getLongitude().doubleValue();

        // A degree of longitude shrinks towards the poles, so the box is wider in longitude than in latitude
        double latitudeDelta = radiusKm / KM_PER_DEGREE;
        double longitudeDelta = radiusKm / (KM_PER_DEGREE * Math.max(Math.cos(Math.toRadians(latitude)), 0.01));

        return villageRepository.findNearby(
                villageId,
                latitude,
                longitude,
                latitude - latitudeDelta,
                latitude + latitudeDelta,
                longitude - longitudeDelta,
                longitude + longitudeDelta,
                radiusKm,
                limit
        );
    }

    private Municipality findMunicipalityIn(Governorate governorate, Long municipalityId) {
        Municipality municipality = municipalityRepository.findById(municipalityId)
                .orElseThrow(() -> new InvalidMunicipalityException(
                        "Municipality with id " + municipalityId + " does not exist"));

        if (!municipality.getGovernorate().getId().equals(governorate.getId())) {
            throw new InvalidMunicipalityException(
                    "Municipality " + municipalityId + " is not in governorate " + governorate.getId());
        }

        return municipality;
    }

    @Transactional
    public Village updateVillage(Long villageId, UpdateVillageRequest request) {
        Village village = villageRepository.findById(villageId)
                .orElseThrow(() -> new VillageNotFoundException(villageId));

        Governorate governorate = governorateRepository.findById(request.governorateId())
                .orElseThrow(() -> new GovernorateNotFoundException(request.governorateId()));

        village.update(
                governorate,
                request.latitude(),
                request.longitude(),
                request.locationDescription(),
                request.elevationM()
        );

        return villageRepository.save(village);
    }

    @Transactional
    public void deleteVillage(Long villageId) {
        Village village = villageRepository.findById(villageId)
                .orElseThrow(() -> new VillageNotFoundException(villageId));

        if (populationRecordRepository.existsByVillageId(villageId)) {
            throw new VillageDeleteConflictException("Cannot delete a village with population records");
        }
        villageRepository.delete(village);
        villageRepository.flush();
    }

    private Sort parseSort(String sort) {
        String[] parts = sort.split(",");
        String field = parts[0].trim();

        if (!SORTABLE_FIELDS.contains(field)) {
            throw new InvalidQueryException(
                    "Cannot sort by '" + field + "'. Allowed fields: "
                            + String.join(", ", new TreeSet<>(SORTABLE_FIELDS)));
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

        return field.equals("id")
                ? sorting
                : sorting.and(Sort.by(Sort.Direction.ASC, "id"));
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
