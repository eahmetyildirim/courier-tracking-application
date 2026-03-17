package com.migros.couriertracking.listener;

import com.migros.couriertracking.event.CourierLocationEvent;
import com.migros.couriertracking.model.CourierLocation;
import com.migros.couriertracking.model.Store;
import com.migros.couriertracking.service.StoreService;
import com.migros.couriertracking.strategy.DistanceStrategy;
import com.migros.couriertracking.strategy.DistanceStrategyRegistry;
import com.migros.couriertracking.strategy.StrategyType;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import com.migros.couriertracking.util.LoggerUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static com.migros.couriertracking.constant.Constants.PROP_DISTANCE_STRATEGY;

/**
 * Listens for courier location events and detects store proximity entries.
 * Delegates business logic to {@link StoreService} for reentry threshold
 * checks and entry recording.
 */
@Component
@RequiredArgsConstructor
public class StoreProximityListener {

    private static final LoggerUtil logger = LoggerUtil.of(StoreProximityListener.class);
    private final StoreService storeService;
    private final DistanceStrategyRegistry strategyRegistry;
    private final MeterRegistry meterRegistry;

    @Value("${courier.store.proximity-meters:100}")
    private double proximityMeters;

    @Value(PROP_DISTANCE_STRATEGY)
    private String strategyTypeName;

    private DistanceStrategy distanceStrategy;

    @PostConstruct
    void init() {
        distanceStrategy = strategyRegistry.getStrategy(StrategyType.fromName(strategyTypeName));
    }

    @EventListener
    @Timed(value = "courier.store.proximity.time", description = "Time spent checking store proximity")
    public void onLocationUpdate(CourierLocationEvent event) {
        CourierLocation location = event.getLocation();

        for (Store store : storeService.findNearbyStores(location.location())) {
            double distance = distanceStrategy.calculate(location.location(), store.location());

            if (distance > proximityMeters) {
                continue;
            }

            if (storeService.isRecentEntry(location.courierId(), store.name(), location.time())) {
                continue;
            }

            storeService.recordStoreEntry(location.courierId(), store.name(), location.time());
            meterRegistry.counter("courier.store.entry", "store", store.name()).increment();
            logger.info("Courier {} ({}) entered {} (distance: {}m)",
                    location.courierId(), event.getCourier().name(),
                    store.name(), String.format("%.2f", distance));
        }
    }
}
