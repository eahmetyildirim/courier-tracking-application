package com.migros.couriertracking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.migros.couriertracking.dto.CourierLocationRequest;
import com.migros.couriertracking.dto.ExampleBearerTokenResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TestingSupportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/testing/example-token - should generate a usable token for a registered courier")
    void shouldGenerateUsableExampleToken() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/testing/example-token")
                        .param("courierId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registered").value(true))
                .andExpect(jsonPath("$.courierName").value("Ahmet Yılmaz"))
                .andReturn();

        ExampleBearerTokenResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ExampleBearerTokenResponse.class
        );

        CourierLocationRequest request = new CourierLocationRequest(
                Instant.parse("2026-03-17T13:00:00Z"), 40.9923307, 29.1244229
        );

        mockMvc.perform(post("/api/courier-locations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + response.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("GET /api/testing/example-token - should mark unknown courier ids as unregistered")
    void shouldReturnUnregisteredForUnknownCourier() throws Exception {
        mockMvc.perform(get("/api/testing/example-token")
                        .param("courierId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courierId").value(999))
                .andExpect(jsonPath("$.registered").value(false))
                .andExpect(jsonPath("$.courierName").value(nullValue()));
    }

    @Test
    @DisplayName("Testing helpers should expose store-entry history and reset in-memory state")
    void shouldExposeHistoryAndResetState() throws Exception {
        mockMvc.perform(post("/api/testing/reset-state"))
                .andExpect(status().isOk());

        ExampleBearerTokenResponse tokenResponse = objectMapper.readValue(
                mockMvc.perform(get("/api/testing/example-token").param("courierId", "1"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                ExampleBearerTokenResponse.class
        );

        CourierLocationRequest first = new CourierLocationRequest(
                Instant.parse("2026-03-17T13:10:00Z"), 40.9923307, 29.1244229
        );
        CourierLocationRequest second = new CourierLocationRequest(
                Instant.parse("2026-03-17T13:10:20Z"), 40.9968, 29.1244
        );

        mockMvc.perform(post("/api/courier-locations")
                        .header(HttpHeaders.AUTHORIZATION, tokenResponse.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/courier-locations")
                        .header(HttpHeaders.AUTHORIZATION, tokenResponse.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/testing/store-entry-history").param("courierId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryCount").value(1))
                .andExpect(jsonPath("$.entries[0].storeName").value("Ataşehir MMM Migros"));

        mockMvc.perform(post("/api/testing/reset-state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("In-memory runtime state cleared."));

        mockMvc.perform(get("/api/testing/store-entry-history").param("courierId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryCount").value(0));

        mockMvc.perform(get("/api/couriers/1/total-distance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDistance").value(0.0));
    }

    @Test
    @DisplayName("OpenAPI docs should include testing helper endpoints")
    void openApiDocsShouldIncludeTestingHelperEndpoints() throws Exception {
        JsonNode root = readOpenApiDocs();
        org.junit.jupiter.api.Assertions.assertTrue(root.path("paths").has("/api/testing/example-token"));
        org.junit.jupiter.api.Assertions.assertTrue(root.path("paths").has("/api/testing/store-entry-history"));
        org.junit.jupiter.api.Assertions.assertTrue(root.path("paths").has("/api/testing/reset-state"));
    }

    @Test
    @DisplayName("OpenAPI docs should expose bearer auth for location reporting")
    void openApiDocsShouldExposeBearerAuthForLocationReporting() throws Exception {
        JsonNode root = readOpenApiDocs();

        JsonNode securityScheme = root.path("components").path("securitySchemes").path("bearerAuth");
        org.junit.jupiter.api.Assertions.assertEquals("http", securityScheme.path("type").asText());
        org.junit.jupiter.api.Assertions.assertEquals("bearer", securityScheme.path("scheme").asText());

        JsonNode postOperation = root.path("paths").path("/api/courier-locations").path("post");
        org.junit.jupiter.api.Assertions.assertTrue(postOperation.path("security").isArray());
        org.junit.jupiter.api.Assertions.assertTrue(postOperation.path("security").get(0).has("bearerAuth"));
    }

    private JsonNode readOpenApiDocs() throws Exception {
        String docsJson = null;
        for (String path : List.of("/api-docs", "/v3/api-docs")) {
            MvcResult result = mockMvc.perform(get(path)).andReturn();
            if (result.getResponse().getStatus() == 200) {
                docsJson = result.getResponse().getContentAsString();
                break;
            }
        }

        org.junit.jupiter.api.Assertions.assertNotNull(docsJson, "OpenAPI docs endpoint was not available");
        return objectMapper.readTree(docsJson);
    }
}
