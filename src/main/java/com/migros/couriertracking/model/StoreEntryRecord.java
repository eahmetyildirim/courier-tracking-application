package com.migros.couriertracking.model;

import java.time.Instant;

public record StoreEntryRecord(Long courierId, String storeName, Instant time) {
}
