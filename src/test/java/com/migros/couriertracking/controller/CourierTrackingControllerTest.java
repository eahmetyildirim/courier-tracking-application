package com.migros.couriertracking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.migros.couriertracking.dto.CourierLocationRequest;
import com.migros.couriertracking.dto.TotalDistanceResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CourierTrackingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/courier-locations - should accept valid location")
    void shouldAcceptValidLocation() throws Exception {
        CourierLocationRequest request = new CourierLocationRequest(
                Instant.now(), 40.9923307, 29.1244229
        );

        mockMvc.perform(post("/api/courier-locations")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenForCourier(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /api/courier-locations - should reject invalid request")
    void shouldRejectInvalidRequest() throws Exception {
        String invalidJson = "{\"time\": null}";

        mockMvc.perform(post("/api/courier-locations")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenForCourier(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/couriers/{id}/total-distance - should return distance")
    void shouldReturnTotalDistance() throws Exception {
        mockMvc.perform(get("/api/couriers/1/total-distance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courierId").value(1))
                .andExpect(jsonPath("$.courierName").value("Ahmet Yılmaz"))
                .andExpect(jsonPath("$.unit").value("meters"));
    }

    @Test
    @DisplayName("POST /api/courier-locations - should reject unknown courier")
    void shouldRejectUnknownCourier() throws Exception {
        CourierLocationRequest request = new CourierLocationRequest(
                Instant.now(), 40.99, 29.12
        );

        mockMvc.perform(post("/api/courier-locations")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenForCourier(999L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/couriers/{id}/total-distance - should return 404 for unknown courier")
    void shouldReturn404ForUnknownCourierDistance() throws Exception {
        mockMvc.perform(get("/api/couriers/999/total-distance"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Full flow: post locations then query distance")
    void fullFlowTest() throws Exception {
        Instant now = Instant.parse("2026-03-17T12:00:00Z");

        CourierLocationRequest loc1 = new CourierLocationRequest(now, 40.99, 29.12);
        mockMvc.perform(post("/api/courier-locations")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenForCourier(2L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loc1)))
                .andExpect(status().isCreated());

        CourierLocationRequest loc2 = new CourierLocationRequest(
                now.plusSeconds(10), 40.991, 29.121
        );
        mockMvc.perform(post("/api/courier-locations")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenForCourier(2L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loc2)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/couriers/2/total-distance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDistance").isNumber());
    }

    @Test
    @DisplayName("POST /api/courier-locations - should reject missing bearer token")
    void shouldRejectMissingBearerToken() throws Exception {
        CourierLocationRequest request = new CourierLocationRequest(
                Instant.now(), 40.9923307, 29.1244229
        );

        mockMvc.perform(post("/api/courier-locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/courier-locations - should reject malformed bearer token")
    void shouldRejectMalformedBearerToken() throws Exception {
        CourierLocationRequest request = new CourierLocationRequest(
                Instant.now(), 40.9923307, 29.1244229
        );

        mockMvc.perform(post("/api/courier-locations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/courier-locations - should reject token without courierId claim")
    void shouldRejectTokenWithoutCourierIdClaim() throws Exception {
        CourierLocationRequest request = new CourierLocationRequest(
                Instant.now(), 40.9923307, 29.1244229
        );

        mockMvc.perform(post("/api/courier-locations")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFromPayload("{\"sub\":\"courier-app\"}"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/courier-locations - duplicate request with same Idempotency-Key should not double count")
    void duplicateRequestWithSameIdempotencyKeyShouldNotDoubleCount() throws Exception {
        Instant now = Instant.parse("2026-03-17T12:10:00Z");

        CourierLocationRequest first = new CourierLocationRequest(now, 40.99, 29.12);
        CourierLocationRequest second = new CourierLocationRequest(now.plusSeconds(10), 40.991, 29.121);

        mockMvc.perform(post("/api/courier-locations")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenForCourier(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/courier-locations")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenForCourier(3L))
                        .header("Idempotency-Key", "dup-location-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isCreated());

        double distanceAfterFirstProcessing = readTotalDistance(3L);

        mockMvc.perform(post("/api/courier-locations")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenForCourier(3L))
                        .header("Idempotency-Key", "dup-location-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isCreated());

        double distanceAfterDuplicate = readTotalDistance(3L);
        org.junit.jupiter.api.Assertions.assertEquals(distanceAfterFirstProcessing, distanceAfterDuplicate);
    }

    @Test
    @DisplayName("POST /api/courier-locations - same Idempotency-Key with different payload should return 409")
    void sameIdempotencyKeyWithDifferentPayloadShouldReturnConflict() throws Exception {
        Instant now = Instant.parse("2026-03-17T12:20:00Z");

        CourierLocationRequest first = new CourierLocationRequest(now, 40.99, 29.12);
        CourierLocationRequest second = new CourierLocationRequest(now.plusSeconds(5), 40.992, 29.122);

        mockMvc.perform(post("/api/courier-locations")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenForCourier(1L))
                        .header("Idempotency-Key", "conflict-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/courier-locations")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenForCourier(1L))
                        .header("Idempotency-Key", "conflict-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isConflict());
    }

    private double readTotalDistance(Long courierId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/couriers/" + courierId + "/total-distance"))
                .andExpect(status().isOk())
                .andReturn();

        TotalDistanceResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), TotalDistanceResponse.class);
        return response.totalDistance();
    }

    private String bearerTokenForCourier(Long courierId) {
        return bearerTokenFromPayload("{\"courierId\":" + courierId + "}");
    }

    private String bearerTokenFromPayload(String payloadJson) {
        String header = base64UrlEncode("{\"alg\":\"none\",\"typ\":\"JWT\"}");
        String payload = base64UrlEncode(payloadJson);
        return "Bearer " + header + "." + payload + ".signature";
    }

    private String base64UrlEncode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
