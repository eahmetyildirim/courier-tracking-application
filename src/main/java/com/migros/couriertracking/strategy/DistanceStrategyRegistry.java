package com.migros.couriertracking.strategy;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Registry for distance calculation strategies.
 * Resolves the appropriate {@link DistanceStrategy} implementation based on {@link StrategyType}.
 *
 * <p>All available strategies are auto-discovered via Spring dependency injection
 * and indexed by their {@link StrategyType} for O(1) lookup.</p>
 */
@Component
public class DistanceStrategyRegistry {

    private final Map<StrategyType, DistanceStrategy> strategyMap;

    public DistanceStrategyRegistry(List<DistanceStrategy> strategies) {
        strategyMap = new EnumMap<>(StrategyType.class);
        strategies.forEach(s -> strategyMap.put(s.getStrategyType(), s));
    }

    /**
     * Retrieve the strategy implementation for the given type.
     *
     * @param type the strategy type to resolve
     * @return the matching {@link DistanceStrategy} implementation
     * @throws IllegalArgumentException if no strategy is registered for the given type
     */
    public DistanceStrategy getStrategy(StrategyType type) {
        DistanceStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No strategy found for type: " + type);
        }
        return strategy;
    }
}
