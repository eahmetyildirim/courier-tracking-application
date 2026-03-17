package com.migros.couriertracking.service;

import com.migros.couriertracking.model.Location;
import com.migros.couriertracking.model.Store;

import java.time.Instant;
import java.util.List;

public interface StoreService {

    List<Store> findNearbyStores(Location location);

    boolean isRecentEntry(Long courierId, String storeName, Instant now);

    void recordStoreEntry(Long courierId, String storeName, Instant time);
}
