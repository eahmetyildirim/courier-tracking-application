package com.migros.couriertracking.service.impl;

import com.migros.couriertracking.dto.CourierLocationRequest;
import com.migros.couriertracking.dto.TotalDistanceResponse;
import com.migros.couriertracking.event.CourierLocationEvent;
import com.migros.couriertracking.idempotency.IdempotencyReservation;
import com.migros.couriertracking.idempotency.LocationRequestIdempotencyService;
import com.migros.couriertracking.exception.CourierNotFoundException;
import com.migros.couriertracking.exception.IdempotencyConflictException;
import com.migros.couriertracking.mapper.CourierMapper;
import com.migros.couriertracking.model.Courier;
import com.migros.couriertracking.model.CourierLocation;
import com.migros.couriertracking.repository.CourierDistanceRepository;
import com.migros.couriertracking.repository.CourierRepository;
import com.migros.couriertracking.service.CourierTrackingService;
import com.migros.couriertracking.exception.ServiceUnavailableException;
import com.migros.couriertracking.util.LoggerUtil;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link CourierTrackingService}.
 * Validates courier existence before processing location updates
 * and publishes domain events for downstream listeners.
 */
@Service
@RequiredArgsConstructor
public class CourierTrackingServiceImpl implements CourierTrackingService {

    private static final LoggerUtil logger = LoggerUtil.of(CourierTrackingServiceImpl.class);
    private final ApplicationEventPublisher eventPublisher;
    private final CourierDistanceRepository distanceRepository;
    private final CourierRepository courierRepository;
    private final CourierMapper courierMapper;
    private final LocationRequestIdempotencyService idempotencyService;

    @Override
    @CircuitBreaker(name = "locationProcessing", fallbackMethod = "locationProcessingFallback")
    public void processLocationUpdate(Long courierId, CourierLocationRequest request, String idempotencyKey) {
        Courier courier = courierRepository.findById(courierId)
                .orElseThrow(() -> new CourierNotFoundException(courierId));

        IdempotencyReservation reservation = idempotencyService.reserve(courierId, idempotencyKey, request);
        if (reservation.duplicate()) {
            logger.info("Skipping duplicate location request for courier={} time={}",
                    courierId, request.time());
            return;
        }

        logger.info("Processing location: courier={} ({}), lat={}, lng={}, time={}",
                courier.id(), courier.name(), request.lat(), request.lng(), request.time());

        try {
            CourierLocation location = courierMapper.toLocation(courierId, request);
            eventPublisher.publishEvent(new CourierLocationEvent(this, location, courier));
            idempotencyService.markCompleted(reservation);
        } catch (RuntimeException ex) {
            idempotencyService.release(reservation);
            throw ex;
        }
    }

    private void locationProcessingFallback(Long courierId, CourierLocationRequest request,
                                               String idempotencyKey, Throwable t) {
        if (t instanceof CourierNotFoundException || t instanceof IdempotencyConflictException) {
            throw (RuntimeException) t;
        }
        logger.warn("Circuit breaker fallback triggered for courier={}: {}", courierId, t.getMessage());
        throw new ServiceUnavailableException("Location processing is temporarily unavailable. Please try again later.");
    }

    @Override
    public TotalDistanceResponse getTotalTravelDistance(Long courierId) {
        Courier courier = courierRepository.findById(courierId)
                .orElseThrow(() -> new CourierNotFoundException(courierId));

        double distance = distanceRepository.getTotalDistance(courierId);
        return new TotalDistanceResponse(courier.id(), courier.name(), distance, "meters");
    }
}
