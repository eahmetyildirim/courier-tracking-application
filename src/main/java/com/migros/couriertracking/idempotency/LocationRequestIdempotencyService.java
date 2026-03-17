package com.migros.couriertracking.idempotency;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.migros.couriertracking.dto.CourierLocationRequest;
import com.migros.couriertracking.exception.IdempotencyConflictException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class LocationRequestIdempotencyService {

    private static final String LOCATION_ENDPOINT = "/api/courier-locations";

    @Value("${courier.idempotency.ttl-seconds:600}")
    private long ttlSeconds;

    private Cache<String, IdempotencyEntry> entries;

    @PostConstruct
    void init() {
        entries = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                .build();
    }

    public IdempotencyReservation reserve(Long courierId, String idempotencyKey, CourierLocationRequest request) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return IdempotencyReservation.untracked();
        }

        String scopedKey = buildScopedKey(courierId, idempotencyKey);
        String fingerprint = fingerprint(courierId, request);
        AtomicReference<Decision> decision = new AtomicReference<>(Decision.NEW);

        entries.asMap().compute(scopedKey, (key, existing) -> {
            if (Objects.isNull(existing)) {
                return new IdempotencyEntry(fingerprint, State.IN_PROGRESS);
            }
            if (!existing.fingerprint().equals(fingerprint)) {
                decision.set(Decision.CONFLICT);
                return existing;
            }
            if (existing.state() == State.COMPLETED) {
                decision.set(Decision.DUPLICATE);
                return existing;
            }
            decision.set(Decision.IN_PROGRESS);
            return existing;
        });

        return switch (decision.get()) {
            case NEW -> IdempotencyReservation.newRequest(scopedKey);
            case DUPLICATE -> IdempotencyReservation.duplicate(scopedKey);
            case CONFLICT -> throw new IdempotencyConflictException(
                    "Idempotency-Key was already used for a different request.");
            case IN_PROGRESS -> throw new IdempotencyConflictException(
                    "A request with the same Idempotency-Key is already in progress.");
        };
    }

    public void markCompleted(IdempotencyReservation reservation) {
        if (!reservation.tracked() || reservation.duplicate()) {
            return;
        }
        entries.asMap().computeIfPresent(reservation.scopedKey(), (key, existing) ->
                new IdempotencyEntry(existing.fingerprint(), State.COMPLETED));
    }

    public void release(IdempotencyReservation reservation) {
        if (!reservation.tracked() || reservation.duplicate()) {
            return;
        }
        entries.invalidate(reservation.scopedKey());
    }

    public void clear() {
        if (entries != null) {
            entries.invalidateAll();
        }
    }

    private String buildScopedKey(Long courierId, String idempotencyKey) {
        return courierId + ":" + LOCATION_ENDPOINT + ":" + idempotencyKey;
    }

    private String fingerprint(Long courierId, CourierLocationRequest request) {
        return courierId + "|" + request.time() + "|" + request.lat() + "|" + request.lng();
    }

    private enum Decision {
        NEW,
        DUPLICATE,
        CONFLICT,
        IN_PROGRESS
    }

    private enum State {
        IN_PROGRESS,
        COMPLETED
    }

    private record IdempotencyEntry(String fingerprint, State state) {
    }
}
