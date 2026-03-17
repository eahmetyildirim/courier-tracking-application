package com.migros.couriertracking.repository;

import com.migros.couriertracking.model.DistanceState;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * In-memory state store for courier distance tracking.
 * Uses {@link ConcurrentHashMap} for atomic updates per courier.
 */
@Repository
public class CourierDistanceRepository {

    private final Map<Long, DistanceState> stateMap = new ConcurrentHashMap<>();

    /**
     * Atomically compute the next distance state for a courier.
     */
    public void atomicUpdate(Long courierId,
                             BiFunction<Long, DistanceState, DistanceState> updater) {
        stateMap.compute(courierId, updater);
    }

    public int getActiveCourierCount() {
        return stateMap.size();
    }

    public double getTotalDistance(Long courierId) {
        DistanceState state = stateMap.get(courierId);
        return Objects.nonNull(state) ? state.totalDistance() : 0.0;
    }

    public void clear() {
        stateMap.clear();
    }
}
