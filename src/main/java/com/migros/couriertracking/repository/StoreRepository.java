package com.migros.couriertracking.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.migros.couriertracking.model.Location;
import com.migros.couriertracking.model.Store;
import com.migros.couriertracking.util.GeoHashUtil;
import com.migros.couriertracking.util.LoggerUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Repository for store locations with geohash-based spatial indexing.
 * Loads store data from {@code stores.json} at startup and builds an
 * in-memory geohash index for efficient proximity queries.
 */
@Repository
public class StoreRepository {

    private static final LoggerUtil logger = LoggerUtil.of(StoreRepository.class);
    private List<Store> stores;
    private Map<String, List<Store>> geohashIndex;

    @PostConstruct
    void init() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = new ClassPathResource("stores.json").getInputStream();
            stores = Collections.unmodifiableList(mapper.readValue(is, new TypeReference<>() {}));

            geohashIndex = GeoHashUtil.buildIndex(stores);

            logger.info("Loaded {} stores into {} geohash cells", stores.size(), geohashIndex.size());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load store data from stores.json", e);
        }
    }

    /**
     * Find candidate stores near the given location using geohash spatial indexing.
     * Queries 9 cells (self + 8 neighbors) for O(1) amortized lookup.
     * Callers should still verify exact distance with Haversine.
     *
     * @param location any {@link Location} entity with geographic coordinates
     * @return list of nearby store candidates
     */
    public List<Store> findNearby(Location location) {
        String[] cells = GeoHashUtil.getSelfAndNeighbors(GeoHashUtil.encode(location.lat(), location.lng()));
        List<Store> candidates = new ArrayList<>();
        for (String cell : cells) {
            List<Store> inCell = geohashIndex.get(cell);
            if (inCell != null) {
                candidates.addAll(inCell);
            }
        }
        return candidates;
    }

}