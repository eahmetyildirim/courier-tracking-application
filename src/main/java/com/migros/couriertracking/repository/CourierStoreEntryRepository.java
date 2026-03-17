package com.migros.couriertracking.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.migros.couriertracking.model.StoreEntryRecord;
import com.migros.couriertracking.util.LoggerUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tracks the last time a courier entered a store's proximity zone.
 * Uses Caffeine cache with TTL for fast deduplication lookups.
 */
@Repository
public class CourierStoreEntryRepository {

    private static final LoggerUtil logger = LoggerUtil.of(CourierStoreEntryRepository.class);

    @Value("${courier.store.reentry-threshold-seconds:60}")
    private long reentryThresholdSeconds;

    private Cache<String, Instant> entryCache;
    private final CopyOnWriteArrayList<StoreEntryRecord> entryHistory = new CopyOnWriteArrayList<>();

    @PostConstruct
    void init() {
        entryCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(reentryThresholdSeconds))
                .evictionListener((String key, Instant value, RemovalCause cause) ->
                        logger.debug("Store entry expired: {} (reason: {})", key, cause))
                .build();

        logger.info("Store entry cache initialized with TTL: {}s", reentryThresholdSeconds);
    }

    public Optional<Instant> getLastEntry(Long courierId, String storeName) {
        return Optional.ofNullable(entryCache.getIfPresent(buildKey(courierId, storeName)));
    }

    public void recordEntry(Long courierId, String storeName, Instant time) {
        entryCache.put(buildKey(courierId, storeName), time);
        entryHistory.add(new StoreEntryRecord(courierId, storeName, time));
    }

    public List<StoreEntryRecord> getEntryHistory(Long courierId) {
        return entryHistory.stream()
                .filter(entry -> entry.courierId().equals(courierId))
                .toList();
    }

    public void clear() {
        entryCache.invalidateAll();
        entryHistory.clear();
    }

    private String buildKey(Long courierId, String storeName) {
        return courierId + ":" + storeName;
    }
}
