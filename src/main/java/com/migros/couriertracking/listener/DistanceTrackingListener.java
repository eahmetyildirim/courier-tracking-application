package com.migros.couriertracking.listener;

import com.migros.couriertracking.event.CourierLocationEvent;
import com.migros.couriertracking.model.CourierLocation;
import com.migros.couriertracking.model.DistanceState;
import com.migros.couriertracking.repository.CourierDistanceRepository;
import com.migros.couriertracking.strategy.DistanceStrategy;
import com.migros.couriertracking.strategy.DistanceStrategyRegistry;
import com.migros.couriertracking.strategy.StrategyType;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import jakarta.annotation.PostConstruct;
import com.migros.couriertracking.util.LoggerUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static com.migros.couriertracking.constant.Constants.PROP_DISTANCE_STRATEGY;

/**
 * Listens for courier location events and accumulates the total travel distance.
 * Uses the configured distance calculation strategy for segment distance computation.
 */
@Component
@RequiredArgsConstructor
public class DistanceTrackingListener {

    private static final LoggerUtil logger = LoggerUtil.of(DistanceTrackingListener.class);
    private final DistanceStrategyRegistry strategyRegistry;
    private final CourierDistanceRepository repository;

    @Value(PROP_DISTANCE_STRATEGY)
    private String strategyTypeName;

    private DistanceStrategy distanceStrategy;

    @PostConstruct
    void init() {
        distanceStrategy = strategyRegistry.getStrategy(StrategyType.fromName(strategyTypeName));
        logger.info("Distance tracking initialized with strategy: {}", strategyTypeName);
    }

    @EventListener
    @Timed(value = "courier.location.processing.time", description = "Time spent processing a location update")
    @Counted(value = "courier.location.processed", description = "Number of location updates processed")
    public void onLocationUpdate(CourierLocationEvent event) {
        CourierLocation location = event.getLocation();
        Long courierId = location.courierId();
        String courierName = event.getCourier().name();

        repository.atomicUpdate(courierId, (key, currentState) -> {
            if (Objects.isNull(currentState)) {
                return new DistanceState(location, 0.0);
            }

            if (location.isOutOfOrderComparedTo(currentState.lastLocation())) {
                logger.debug("Ignoring out-of-order location for courier {} ({}) at {}",
                        courierId, courierName, location.time());
                return currentState;
            }

            double distance = distanceStrategy.calculate(currentState.lastLocation().location(), location.location());
            double updatedTotal = currentState.totalDistance() + distance;

            logger.debug("Courier {} ({}) moved {}m, total: {}m",
                    courierId, courierName,
                    String.format("%.2f", distance), String.format("%.2f", updatedTotal));

            return new DistanceState(location, updatedTotal);
        });
    }
}
