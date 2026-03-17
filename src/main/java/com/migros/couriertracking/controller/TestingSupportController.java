package com.migros.couriertracking.controller;

import com.migros.couriertracking.auth.CourierTokenAuthService;
import com.migros.couriertracking.dto.ExampleBearerTokenResponse;
import com.migros.couriertracking.dto.StoreEntryHistoryResponse;
import com.migros.couriertracking.dto.TestingResetResponse;
import com.migros.couriertracking.idempotency.LocationRequestIdempotencyService;
import com.migros.couriertracking.model.Courier;
import com.migros.couriertracking.model.StoreEntryRecord;
import com.migros.couriertracking.repository.CourierDistanceRepository;
import com.migros.couriertracking.repository.CourierRepository;
import com.migros.couriertracking.repository.CourierStoreEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequestMapping("/api/testing")
@RequiredArgsConstructor
public class TestingSupportController implements TestingSupportApi {

    private static final String USAGE_HINT =
            "Paste the token field into Swagger Authorize. Use authorizationHeader directly in Postman or curl.";
    private static final String RESET_MESSAGE = "In-memory runtime state cleared.";

    private final CourierTokenAuthService courierTokenAuthService;
    private final CourierRepository courierRepository;
    private final CourierDistanceRepository courierDistanceRepository;
    private final CourierStoreEntryRepository courierStoreEntryRepository;
    private final LocationRequestIdempotencyService locationRequestIdempotencyService;

    @Override
    @GetMapping("/example-token")
    public ResponseEntity<ExampleBearerTokenResponse> generateExampleToken(
            @RequestParam(defaultValue = "1") Long courierId) {
        Courier courier = courierRepository.findById(courierId).orElse(null);

        return ResponseEntity.ok(new ExampleBearerTokenResponse(
                courierId,
                courier != null,
                courier != null ? courier.name() : null,
                courierTokenAuthService.createExampleToken(courierId),
                courierTokenAuthService.createAuthorizationHeader(courierId),
                USAGE_HINT
        ));
    }

    @Override
    @GetMapping("/store-entry-history")
    public ResponseEntity<StoreEntryHistoryResponse> getStoreEntryHistory(
            @RequestParam Long courierId) {
        List<StoreEntryHistoryResponse.Entry> entries = courierStoreEntryRepository.getEntryHistory(courierId).stream()
                .map(this::toResponseEntry)
                .toList();

        return ResponseEntity.ok(new StoreEntryHistoryResponse(courierId, entries.size(), entries));
    }

    @Override
    @PostMapping("/reset-state")
    public ResponseEntity<TestingResetResponse> resetState() {
        courierDistanceRepository.clear();
        courierStoreEntryRepository.clear();
        locationRequestIdempotencyService.clear();
        return ResponseEntity.ok(new TestingResetResponse(RESET_MESSAGE));
    }

    private StoreEntryHistoryResponse.Entry toResponseEntry(StoreEntryRecord entry) {
        return new StoreEntryHistoryResponse.Entry(entry.storeName(), entry.time());
    }
}
