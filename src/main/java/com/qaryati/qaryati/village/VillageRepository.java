package com.qaryati.qaryati.village;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VillageRepository extends JpaRepository<Village, Long>, JpaSpecificationExecutor<Village> {

    /**
     * Villages within radiusKm of a point, nearest first, using the Haversine formula (Earth radius 6371 km).
     * The latitude/longitude box is a cheap pre-filter so the distance is only computed for nearby rows.
     * LEAST(1, ...) guards ASIN against rounding errors just above 1.
     */
    @Query(value = """
            SELECT *
            FROM (
                SELECT
                    v.id                   AS "id",
                    g.name                 AS "governorateName",
                    v.latitude             AS "latitude",
                    v.longitude            AS "longitude",
                    v.location_description AS "locationDescription",
                    2 * 6371 * ASIN(LEAST(1, SQRT(
                        POWER(SIN(RADIANS(v.latitude - :latitude) / 2), 2)
                        + COS(RADIANS(:latitude)) * COS(RADIANS(v.latitude))
                        * POWER(SIN(RADIANS(v.longitude - :longitude) / 2), 2)
                    )))                    AS "distanceKm"
                FROM villages v
                JOIN governorates g ON g.id = v.governorate_id
                WHERE v.id <> :villageId
                  AND v.latitude BETWEEN :minLatitude AND :maxLatitude
                  AND v.longitude BETWEEN :minLongitude AND :maxLongitude
            ) nearby
            WHERE "distanceKm" <= :radiusKm
            ORDER BY "distanceKm", "id"
            LIMIT :limit
            """, nativeQuery = true)
    List<NearbyVillageRow> findNearby(
            @Param("villageId") Long villageId,
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("minLatitude") double minLatitude,
            @Param("maxLatitude") double maxLatitude,
            @Param("minLongitude") double minLongitude,
            @Param("maxLongitude") double maxLongitude,
            @Param("radiusKm") double radiusKm,
            @Param("limit") int limit
    );
}
