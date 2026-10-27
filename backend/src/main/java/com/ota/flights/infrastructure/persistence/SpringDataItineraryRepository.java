package com.ota.flights.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface SpringDataItineraryRepository extends JpaRepository<ItineraryEntity, String> {

    @Modifying
    @Query(value = "TRUNCATE TABLE itineraries", nativeQuery = true)
    void truncate();

    @Query(value = """
            SELECT * FROM itineraries
            WHERE (:origin IS NULL OR origin = :origin)
              AND (:destination IS NULL OR destination = :destination)
              AND (:maxPrice IS NULL OR final_price <= :maxPrice)
              AND (:carrier IS NULL OR :carrier = ANY(carriers))
              AND (:maxStops IS NULL OR stops <= :maxStops)
              AND (:cabinClass IS NULL OR cabin_class = :cabinClass)
              AND (
                    :cursorPrice IS NULL
                    OR final_price > :cursorPrice
                    OR (final_price = :cursorPrice AND id > :cursorId)
                  )
            ORDER BY final_price ASC, id ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<ItineraryEntity> searchKeyset(
            @Param("origin") String origin,
            @Param("destination") String destination,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("carrier") String carrier,
            @Param("maxStops") Integer maxStops,
            @Param("cabinClass") String cabinClass,
            @Param("cursorPrice") BigDecimal cursorPrice,
            @Param("cursorId") String cursorId,
            @Param("limit") int limit
    );

    @Query(value = """
            SELECT COUNT(*) FROM itineraries
            WHERE (:origin IS NULL OR origin = :origin)
              AND (:destination IS NULL OR destination = :destination)
              AND (:maxPrice IS NULL OR final_price <= :maxPrice)
              AND (:carrier IS NULL OR :carrier = ANY(carriers))
            """, nativeQuery = true)
    long countFiltered(
            @Param("origin") String origin,
            @Param("destination") String destination,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("carrier") String carrier
    );
}
