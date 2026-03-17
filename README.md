# Migros Courier Tracking Application

A Spring Boot service for the Migros courier tracking case. It accepts courier location updates, records store proximity entries, and returns each courier's total travel distance.

## Requirement Coverage

This implementation covers the case requirements as follows:

- **Location ingestion**: `POST /api/courier-locations` accepts `time`, `lat`, and `lng`.
- **Store entry detection**: when a courier enters the `100m` radius of a Migros store, the system records that entry.
- **Reentry rule**: reentries to the same store within `60 seconds` are not counted as a new entrance.
- **Total distance query**: `GET /api/couriers/{courierId}/total-distance` returns the accumulated travel distance in meters.
- **Store data source**: store locations come from `src/main/resources/stores.json`.
- **Design patterns used**:
  - `Strategy`: distance calculation strategies
  - `Event Listener / Observer`: location event publishing and synchronous listeners
  - `Repository`: in-memory access to courier, store, distance, and entry state

## Solution Scope

This version is intentionally `prod-lite`:

- reference data comes from `src/main/resources/couriers.json` and `src/main/resources/stores.json`
- request processing is deterministic and synchronous
- no database is used in this iteration
- duplicate `POST` retries can be protected with an optional `Idempotency-Key`
- `POST /api/courier-locations` takes courier identity from a lightweight Bearer token claim
- the codebase includes an optional circuit breaker as an operational safeguard example, not as a core case requirement

## Prerequisites

- `Java 21+`
- `Maven 3.8+`
- or `Docker`

## Run

### Docker

```bash
docker compose up -d
```

### Maven

```bash
mvn spring-boot:run

# or
mvn clean package -DskipTests
java -jar target/courier-tracking-1.0.0.jar
```

Application URL: `http://localhost:8080`

Swagger UI: `http://localhost:8080/swagger-ui.html`

Postman collection: `postman/courier-tracking.postman_collection.json`

## Core API

### Report Courier Location

```http
POST /api/courier-locations
Authorization: Bearer <token>
Content-Type: application/json
Idempotency-Key: <optional>

{
  "time": "2024-01-15T10:30:00Z",
  "lat": 40.9923307,
  "lng": 29.1244229
}
```

Rules:

- `Authorization` is required for `POST`
- the token must be JWT-like: `header.payload.signature`
- only the payload is decoded
- the payload must contain `courierId`
- unknown `courierId` returns `404 Not Found`
- missing or malformed token returns `401 Unauthorized`
- `Idempotency-Key` is optional
- the same `Idempotency-Key` with the same effective request returns `201 Created` without double-processing
- the same `Idempotency-Key` with a different effective request returns `409 Conflict`

Authentication note:

- this is a deliberate simplification for the case
- the service does **not** validate token signature, issuer, audience, or expiration
- in a real production system this would be replaced by verified authentication from an identity provider or gateway

### Get Total Travel Distance

```http
GET /api/couriers/{courierId}/total-distance
```

Example response:

```json
{
  "courierId": 1,
  "courierName": "Ahmet Yılmaz",
  "totalDistance": 1523.45,
  "unit": "meters"
}
```

Notes:

- this endpoint remains public in this iteration
- unknown couriers return `404 Not Found`

## Testing Utilities

These endpoints exist only for local verification in Swagger and Postman.
They are **not** part of the core business API and should not be exposed in a real production deployment.

### Generate Example Bearer Token

```http
GET /api/testing/example-token?courierId=1
```

Purpose:

- generate a Swagger/Postman-friendly Bearer token for local testing
- return both the raw `token` and the full `authorizationHeader`
- indicate whether the given courier id exists in `couriers.json`

Swagger usage:

1. Call `GET /api/testing/example-token`
2. Copy the `token` field
3. Click `Authorize` in Swagger UI
4. Paste the token value
5. Call `POST /api/courier-locations`

Example token payload:

```json
{
  "courierId": 1
}
```

### Runtime State Helpers

```http
POST /api/testing/reset-state
GET /api/testing/store-entry-history?courierId=1
```

Purpose:

- `reset-state`: clears in-memory runtime state before an isolated scenario run
- `store-entry-history`: returns recorded store-entry events for one courier so case scenarios can be verified from Postman and Swagger

## Business Behavior

- **Distance tracking**: total distance is accumulated incrementally per courier
- **Store proximity detection**: store lookup uses a geohash index and a configurable radius of `100m` by default
- **Reentry deduplication**: the same courier entering the same store again within the configured threshold of `60s` is not logged as a new entrance
- **Reference data**: couriers and stores are loaded from JSON resources at application startup

## Technical Behavior

- **Deterministic processing**: a successful `POST` is fully processed before the response returns
- **Immediate consistency**: an immediate `GET total-distance` sees the latest accepted location update
- **Out-of-order protection**: older timestamps do not move the courier state backwards
- **Idempotency**: repeated `POST` retries can be safely ignored when the same `Idempotency-Key` is reused with the same effective request

## Architecture

```text
HTTP Request
    |
    v
Controller
    |
    +--> Bearer token parsing (courierId claim)
    +--> request validation
    |
    v
Service
    |
    +--> courier existence check
    +--> optional idempotency reservation
    +--> optional circuit breaker guard
    |
    v
CourierLocationEvent
    |
    +--> DistanceTrackingListener --> CourierDistanceRepository (ConcurrentHashMap)
    |
    +--> StoreProximityListener --> StoreRepository (JSON + geohash index)
                                 --> CourierStoreEntryRepository (Caffeine TTL cache)
```

The event-driven separation is kept, but listeners run synchronously inside the request thread.

## Configuration

Main settings in `src/main/resources/application.yml`:

| Property | Default | Description |
|----------|---------|-------------|
| `courier.distance.strategy` | `HAVERSINE` | Distance calculation algorithm |
| `courier.store.proximity-meters` | `100` | Store entry detection radius in meters |
| `courier.store.reentry-threshold-seconds` | `60` | Duplicate store-entry threshold |
| `courier.idempotency.ttl-seconds` | `600` | TTL for stored idempotency keys |

Optional operational guard settings in `src/main/resources/resilience4j.yml`:

| Property | Default | Description |
|----------|---------|-------------|
| `sliding-window-size` | `10` | Number of calls in the sliding window |
| `failure-rate-threshold` | `50` | Failure percentage to trip the circuit breaker |
| `wait-duration-in-open-state` | `30s` | Time before transitioning to half-open |
| `permitted-number-of-calls-in-half-open-state` | `3` | Probe calls allowed in half-open |
| `sliding-window-type` | `COUNT_BASED` | Window type |

Business exceptions such as `CourierNotFoundException`, `IdempotencyConflictException`, and `UnauthorizedRequestException` are excluded from the failure count.

## Testing

```bash
mvn clean test
```

The automated test suite covers:

- controller flow and request validation
- Bearer token parsing and authorization errors
- idempotency behavior
- distance accumulation and out-of-order handling
- store proximity and reentry deduplication
- geohash utilities and strategy selection
- Swagger helper endpoints and OpenAPI exposure

## cURL Examples

```bash
TOKEN='eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJjb3VyaWVySWQiOjF9.signature'

# 1. Courier starts near Ataşehir MMM Migros
curl -X POST http://localhost:8080/api/courier-locations \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"time":"2024-01-15T10:00:00Z","lat":40.9924,"lng":29.1245}'

# 2. Courier moves to another location
curl -X POST http://localhost:8080/api/courier-locations \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: loc-2" \
  -d '{"time":"2024-01-15T10:05:00Z","lat":40.9860,"lng":29.1160}'

# 3. Duplicate retry with the same idempotency key is ignored safely
curl -X POST http://localhost:8080/api/courier-locations \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: loc-2" \
  -d '{"time":"2024-01-15T10:05:00Z","lat":40.9860,"lng":29.1160}'

# 4. Check total distance
curl http://localhost:8080/api/couriers/1/total-distance
```

## Postman

Collection file: `postman/courier-tracking.postman_collection.json`

The collection includes these case-oriented scenarios:

- `Scenario 1 - Store Entry Is Recorded`
- `Scenario 2 - Reentry Within 1 Minute Is Ignored`
- `Scenario 3 - Reentry After 60 Seconds Is Recorded`
- `Scenario 4 - Same Courier Enters Two Different Stores`
- `Scenario 5 - Total Distance Is Calculated`

Collection behavior:

- every scenario starts with `POST /api/testing/reset-state`
- every scenario gets a fresh Bearer token from `GET /api/testing/example-token`
- store-entry scenarios verify results through `GET /api/testing/store-entry-history`
- `baseUrl` is prefilled as `http://localhost:8080`

## Observability

- **Metrics**: Micrometer counters and timers on distance tracking and store entry events
- **Prometheus**: `GET /actuator/prometheus`
- **Health**: `GET /actuator/health`
- **Request tracing**: each request gets a unique `X-Request-Id` header and MDC context

## Tech Stack

- `Java 21`
- `Spring Boot 3.2.4`
- `Spring MVC`
- `Spring Validation`
- `Springdoc OpenAPI / Swagger UI`
- `Caffeine`
- `Micrometer + Prometheus`
- `Resilience4j`
- `JUnit 5 + MockMvc`
