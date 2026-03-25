# Migros Courier Tracking Application

Bu proje, Migros courier tracking case'i için geliştirilmiş bir Spring Boot servistir. Courier location update alır, store proximity entry kayıtlarını tutar ve her courier için total travel distance bilgisini döner.

## Requirement Coverage

Bu implementasyon case requirement'larını şu şekilde karşılar:

- **Location ingestion**: `POST /api/courier-locations` endpoint'i `time`, `lat` ve `lng` alanlarını kabul eder.
- **Store entry detection**: bir courier, Migros mağazasının `100m` radius alanına girdiğinde sistem bu entry'yi kaydeder.
- **Reentry rule**: aynı mağaza için `60 seconds` içinde gerçekleşen reentry yeni bir entrance olarak sayılmaz.
- **Total distance query**: `GET /api/couriers/{courierId}/total-distance` endpoint'i bir courier'ın biriktirdiği travel distance bilgisini meter cinsinden döner.
- **Store data source**: store location verileri `src/main/resources/stores.json` dosyasından yüklenir.
- **Design patterns used**:
  - `Strategy`: distance calculation strategy seçimi
  - `Event Listener / Observer`: location event publish edilmesi ve synchronous listener'lar
  - `Repository`: courier, store, distance ve entry state erişimi

## Solution Scope

Bu versiyon bilinçli olarak `case` tutuldu:

- reference data `src/main/resources/couriers.json` ve `src/main/resources/stores.json` dosyalarından gelir
- request processing deterministic ve synchronous çalışır
- bu iterasyonda database kullanılmaz
- duplicate `POST` retry durumları optional `Idempotency-Key` ile korunabilir
- `POST /api/courier-locations` endpoint'i courier identity bilgisini lightweight bir Bearer token claim'inden alır
- codebase içinde optional bir circuit breaker örneği bulunur; bu yapı core case requirement değil, operational safeguard örneğidir

## Prerequisites

- `Java 21+`
- `Maven 3.8+`
- veya `Docker`

## Run

### Docker

```bash
docker compose up -d
```

### Maven

```bash
mvn spring-boot:run

# veya
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

- `POST` için `Authorization` zorunludur
- token formatı JWT-like olmalıdır: `header.payload.signature`
- sadece payload decode edilir
- payload içinde `courierId` bulunmalıdır
- unknown `courierId` için `404 Not Found` döner
- missing veya malformed token için `401 Unauthorized` döner
- `Idempotency-Key` optional'dır
- aynı `Idempotency-Key`, aynı effective request ile tekrar gelirse `201 Created` döner ve request yeniden işlenmez
- aynı `Idempotency-Key`, farklı bir effective request ile tekrar gelirse `409 Conflict` döner

Authentication note:

- bu yaklaşım case için bilinçli bir simplification'dır
- servis token signature, issuer, audience veya expiration validate etmez
- gerçek bir production sistemde bu yapı verified authentication ile, bir identity provider veya gateway üzerinden çözülmelidir

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

- bu iterasyonda endpoint public bırakılmıştır
- unknown courier için `404 Not Found` döner

## Testing Utilities

Bu endpoint'ler sadece local verification amacıyla Swagger ve Postman kullanımını kolaylaştırmak için eklenmiştir.
Bunlar core business API'nin parçası değildir ve gerçek production deployment içinde exposed edilmemelidir.

### Generate Example Bearer Token

```http
GET /api/testing/example-token?courierId=1
```

Purpose:

- local test için Swagger/Postman uyumlu bir Bearer token üretmek
- hem raw `token` hem de tam `authorizationHeader` değerini döndürmek
- verilen courier id'nin `couriers.json` içinde kayıtlı olup olmadığını göstermek

Swagger usage:

1. `GET /api/testing/example-token` çağrısını yap
2. `token` alanını kopyala
3. Swagger UI içinde `Authorize` butonuna tıkla
4. token değerini yapıştır
5. `POST /api/courier-locations` çağrısını yap

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

- `reset-state`: izole bir scenario çalıştırmadan önce in-memory runtime state'i temizler
- `store-entry-history`: bir courier için kaydedilmiş store-entry event'lerini döner; böylece case scenario'ları Postman ve Swagger üzerinden doğrulanabilir

## Business Behavior

- **Distance tracking**: total distance bilgisi courier bazında incremental olarak biriktirilir
- **Store proximity detection**: store lookup, geohash index ve default `100m` radius ile çalışır
- **Reentry deduplication**: aynı courier aynı store'a `60s` threshold içinde tekrar girerse yeni bir entrance olarak loglanmaz
- **Reference data**: couriers ve stores, application startup sırasında JSON resource'lardan yüklenir

## Technical Behavior

- **Deterministic processing**: başarılı bir `POST`, response dönmeden önce tamamen işlenir
- **Immediate consistency**: başarılı bir `POST` sonrasında yapılan anlık `GET total-distance` çağrısı en güncel sonucu görür
- **Out-of-order protection**: daha eski timestamp'ler courier state'ini geriye götürmez
- **Idempotency**: aynı `Idempotency-Key` ile gelen tekrar `POST` çağrıları güvenli şekilde ignore edilebilir

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

Event-driven ayrım korunmuştur, ancak listener'lar request thread'i içinde synchronous çalışır.

## Configuration

Ana ayarlar `src/main/resources/application.yml` içinde bulunur:

| Property | Default | Description |
|----------|---------|-------------|
| `courier.distance.strategy` | `HAVERSINE` | Distance calculation algorithm |
| `courier.store.proximity-meters` | `100` | Store entry detection radius in meters |
| `courier.store.reentry-threshold-seconds` | `60` | Duplicate store-entry threshold |
| `courier.idempotency.ttl-seconds` | `600` | TTL for stored idempotency keys |

Optional operational guard ayarları `src/main/resources/resilience4j.yml` içinde bulunur:

| Property | Default | Description |
|----------|---------|-------------|
| `sliding-window-size` | `10` | Number of calls in the sliding window |
| `failure-rate-threshold` | `50` | Failure percentage to trip the circuit breaker |
| `wait-duration-in-open-state` | `30s` | Time before transitioning to half-open |
| `permitted-number-of-calls-in-half-open-state` | `3` | Probe calls allowed in half-open |
| `sliding-window-type` | `COUNT_BASED` | Window type |

`CourierNotFoundException`, `IdempotencyConflictException` ve `UnauthorizedRequestException` gibi business exception'lar failure count dışında tutulur.

## Testing

```bash
mvn clean test
```

Automated test suite şu alanları kapsar:

- controller flow ve request validation
- Bearer token parsing ve authorization error senaryoları
- idempotency behavior
- distance accumulation ve out-of-order handling
- store proximity ve reentry deduplication
- geohash utility ve strategy selection
- Swagger helper endpoint'leri ve OpenAPI exposure

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

Collection dosyası: `postman/courier-tracking.postman_collection.json`

Collection içinde şu case-oriented scenario'lar bulunur:

- `Scenario 1 - Store Entry Is Recorded`
- `Scenario 2 - Reentry Within 1 Minute Is Ignored`
- `Scenario 3 - Reentry After 60 Seconds Is Recorded`
- `Scenario 4 - Same Courier Enters Two Different Stores`
- `Scenario 5 - Total Distance Is Calculated`

Collection behavior:

- her scenario `POST /api/testing/reset-state` ile başlar
- her scenario `GET /api/testing/example-token` ile yeni bir Bearer token alır
- store-entry scenario'ları sonucu `GET /api/testing/store-entry-history` ile doğrular
- `baseUrl` değişkeni default olarak `http://localhost:8080` şeklinde set edilmiştir

## Observability

- **Metrics**: distance tracking ve store entry event'leri için Micrometer counter ve timer'lar
- **Prometheus**: `GET /actuator/prometheus`
- **Health**: `GET /actuator/health`
- **Request tracing**: her request için benzersiz bir `X-Request-Id` header'ı ve MDC context oluşturulur

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
