package com.migros.couriertracking.service.impl;

import com.migros.couriertracking.model.Location;
import com.migros.couriertracking.model.Store;
import com.migros.couriertracking.repository.CourierStoreEntryRepository;
import com.migros.couriertracking.repository.StoreRepository;
import com.migros.couriertracking.service.StoreService;
import com.migros.couriertracking.util.LoggerUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Default implementation of {@link StoreService}.
 * Manages store proximity business logic including reentry threshold checks,
 * delegating pure data access to repositories.
 */
@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private static final LoggerUtil logger = LoggerUtil.of(StoreServiceImpl.class);

    private final StoreRepository storeRepository;
    private final CourierStoreEntryRepository entryRepository;

    @Value("${courier.store.reentry-threshold-seconds:60}")
    private long reentryThresholdSeconds;

    @Override
    public List<Store> findNearbyStores(Location location) {
        return storeRepository.findNearby(location);
    }

    @Override
    public boolean isRecentEntry(Long courierId, String storeName, Instant now) {
        Optional<Instant> lastEntry = entryRepository.getLastEntry(courierId, storeName);
        if (lastEntry.isEmpty()) {
            return false;
        }
        boolean isRecent = now.minusSeconds(reentryThresholdSeconds).isBefore(lastEntry.get());
        if (isRecent) {
            logger.debug("Courier {} re-entered {} within {}s threshold, skipping",
                    courierId, storeName, reentryThresholdSeconds);
        }
        return isRecent;
    }

    @Override
    public void recordStoreEntry(Long courierId, String storeName, Instant time) {
        entryRepository.recordEntry(courierId, storeName, time);
        logger.info("Recorded store entry: courier={}, store={}, time={}", courierId, storeName, time);
    }
}
