package com.migros.couriertracking.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.migros.couriertracking.model.Courier;
import jakarta.annotation.PostConstruct;
import com.migros.couriertracking.util.LoggerUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Repository for registered couriers.
 * Loads courier data from {@code couriers.json} at startup and provides
 * id-based lookup for courier validation.
 */
@Repository
public class CourierRepository {

    private static final LoggerUtil logger = LoggerUtil.of(CourierRepository.class);
    private Map<Long, Courier> couriersById;

    @PostConstruct
    void init() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = new ClassPathResource("couriers.json").getInputStream();
            List<Courier> couriers = mapper.readValue(is, new TypeReference<>() {});

            couriersById = couriers.stream()
                    .collect(Collectors.toUnmodifiableMap(Courier::id, Function.identity()));

            logger.info("Loaded {} registered couriers", couriersById.size());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load courier data from couriers.json", e);
        }
    }

    public Optional<Courier> findById(Long id) {
        return Optional.ofNullable(couriersById.get(id));
    }

}
