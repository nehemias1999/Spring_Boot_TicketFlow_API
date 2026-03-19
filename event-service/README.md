# event-service

![Java 21](https://img.shields.io/badge/Java-21-blue)
![Spring Boot 3.5.4](https://img.shields.io/badge/Spring%20Boot-3.5.4-brightgreen)
![Spring Cloud 2025.0.0](https://img.shields.io/badge/Spring%20Cloud-2025.0.0-brightgreen)

Microservice responsible for managing events in the **TicketFlow** ticket reservation system. Exposes a REST API consumed by the API Gateway to create, read, update, and soft-delete events.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Tech Stack](#tech-stack)
4. [API Endpoints](#api-endpoints)
5. [Request & Response Models](#request--response-models)
6. [Database Schema](#database-schema)
7. [Configuration](#configuration)
8. [Running the Service](#running-the-service)
9. [Running Tests](#running-tests)
10. [Health & Monitoring](#health--monitoring)

---

## Overview

`event-service` manages the event catalog for the TicketFlow platform. It provides full CRUD operations with soft-delete support, paginated and filterable listings, and automatic schema migrations via Flyway. Event IDs are server-generated UUIDs. The service registers itself with Eureka and is accessible through the API Gateway.

---

## Architecture

The service follows **Hexagonal Architecture (Ports & Adapters)** combined with **Vertical Slicing**, keeping domain logic isolated from infrastructure concerns.

```
┌──────────────────────────────────────────────────────────┐
│                    Inbound Adapter                        │
│         REST Controller  (EventController)                │
│         /api/v1/events                                    │
└────────────────────────┬─────────────────────────────────┘
                         │ uses port in
┌────────────────────────▼─────────────────────────────────┐
│                   Application Layer                       │
│   EventService  │  DTOs (Create/Update/Response)          │
│   MapStruct Mapper  │  Jakarta Validation                 │
└────────────────────────┬─────────────────────────────────┘
                         │ uses port out
┌────────────────────────▼─────────────────────────────────┐
│                     Domain Layer                          │
│   Event model  │  IEventService (port in)                 │
│   IEventPersistencePort (port out)  │  Exceptions         │
└────────────────────────┬─────────────────────────────────┘
                         │ implements port out
┌────────────────────────▼─────────────────────────────────┐
│                   Outbound Adapter                        │
│   EventPersistenceAdapter  │  EventEntity                 │
│   IEventJpaRepository (Spring Data JPA)                   │
│   EventSpecification (dynamic filters)                    │
│   Flyway migrations  │  MySQL 8                           │
└──────────────────────────────────────────────────────────┘
```

### Package overview

| Package | Responsibility |
|---------|---------------|
| `catalog.infrastructure.adapter.in.web` | REST controllers — inbound adapters |
| `catalog.application.service` | Business logic — orchestrates domain operations |
| `catalog.application.dto` | Request/response DTOs and MapStruct mappers |
| `catalog.domain.model` | Core domain model (`Event`) |
| `catalog.domain.port.in` | Inbound port interfaces (`IEventService`) |
| `catalog.domain.port.out` | Outbound port interfaces (`IEventPersistencePort`) |
| `catalog.domain.exception` | Domain exceptions (`EventNotFoundException`) |
| `catalog.infrastructure.adapter.out.persistence` | JPA entities, repositories, persistence adapter |
| `shared.infrastructure.exception` | Global exception handler and `ApiErrorResponse` |
| `shared.infrastructure.filter` | `CorrelationIdFilter` — MDC and response header |
| `shared.infrastructure.config` | JPA auditing configuration |

---

## Tech Stack

| Category | Technology |
|----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.4 |
| Cloud | Spring Cloud 2025.0.0 (Config, Eureka, LoadBalancer) |
| Persistence | Spring Data JPA, Hibernate, MySQL 8, Flyway |
| Mapping | MapStruct 1.6.3 |
| Validation | Jakarta Bean Validation |
| API Docs | springdoc-openapi 2.8.8 (Swagger UI) |
| Monitoring | Spring Boot Actuator |
| Testing | JUnit 5, Mockito, H2 (in-memory) |
| Build | Maven |
| Utils | Lombok |

---

## API Endpoints

| Method | Path | Description | Request Body | Response |
|--------|------|-------------|-------------|----------|
| `POST` | `/api/v1/events` | Create a new event | `CreateEventRequest` | `201 EventResponse` |
| `GET` | `/api/v1/events/{id}` | Get event by ID | — | `200 EventResponse` |
| `GET` | `/api/v1/events` | List events (paginated + filtered) | — | `200 Page<EventResponse>` |
| `PUT` | `/api/v1/events/{id}` | Update an existing event | `UpdateEventRequest` | `200 EventResponse` |
| `DELETE` | `/api/v1/events/{id}` | Soft-delete an event | — | `204 No Content` |

Interactive documentation is available via Swagger UI at `http://localhost:8081/swagger-ui.html`.

---

### POST `/api/v1/events`

Creates a new event. The ID is generated server-side as a UUID.

- **201 Created** — event created successfully, returns `EventResponse`
- **400 Bad Request** — validation failure

---

### GET `/api/v1/events/{id}`

Retrieves a single active event by its UUID.

- **200 OK** — returns `EventResponse`
- **404 Not Found** — no active event with that ID

---

### GET `/api/v1/events`

Returns a paginated list of active events with optional filters. Page size is capped at 100.

| Query Parameter | Type | Default | Description |
|-----------------|------|---------|-------------|
| `page` | int | `0` | Page number (zero-based) |
| `size` | int | `10` | Items per page (max 100) |
| `title` | String | — | Filter by title (contains, case-insensitive) |
| `location` | String | — | Filter by location (contains, case-insensitive) |
| `sortBy` | String | `createdAt` | Field to sort by |
| `sortDir` | String | `desc` | Sort direction: `asc` or `desc` |

- **200 OK** — returns `Page<EventResponse>`

---

### PUT `/api/v1/events/{id}`

Replaces all mutable fields of an existing event.

- **200 OK** — returns updated `EventResponse`
- **404 Not Found** — event does not exist
- **400 Bad Request** — validation failure

---

### DELETE `/api/v1/events/{id}`

Soft-deletes an event. The record is marked `deleted = true` and excluded from all active queries. The row is never physically removed.

- **204 No Content** — deleted successfully
- **404 Not Found** — event does not exist

---

## Request & Response Models

### `CreateEventRequest`

```json
{
  "title":       "Lollapalooza 2026",
  "description": "Annual music festival in Chicago",
  "date":        "2026-08-01 16:00",
  "location":    "Grant Park, Chicago, IL",
  "basePrice":   99.99
}
```

| Field | Type | Constraints |
|-------|------|-------------|
| `title` | String | Required, 3–150 characters |
| `description` | String | Required, max 500 characters |
| `date` | String | Required |
| `location` | String | Required, max 200 characters |
| `basePrice` | BigDecimal | Required, ≥ 0, max 10 integer digits and 2 decimal places |

---

### `UpdateEventRequest`

```json
{
  "title":       "Lollapalooza 2026 — Updated",
  "description": "Updated description",
  "date":        "2026-08-02 17:00",
  "location":    "Grant Park, Chicago, IL",
  "basePrice":   119.99
}
```

| Field | Type | Constraints |
|-------|------|-------------|
| `title` | String | Required, 3–150 characters |
| `description` | String | Required, max 500 characters |
| `date` | String | Required |
| `location` | String | Required, max 200 characters |
| `basePrice` | BigDecimal | Required, ≥ 0, max 10 integer digits and 2 decimal places |

---

### `EventResponse`

```json
{
  "id":          "550e8400-e29b-41d4-a716-446655440000",
  "title":       "Lollapalooza 2026",
  "description": "Annual music festival in Chicago",
  "date":        "2026-08-01 16:00",
  "location":    "Grant Park, Chicago, IL",
  "basePrice":   99.99,
  "createdAt":   "2026-03-11T10:00:00",
  "updatedAt":   "2026-03-11T12:00:00"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Server-generated UUID |
| `title` | String | Event name |
| `description` | String | Short summary |
| `date` | String | Date and time of the event |
| `location` | String | Venue |
| `basePrice` | BigDecimal | Base reference price |
| `createdAt` | LocalDateTime | Creation timestamp |
| `updatedAt` | LocalDateTime | Last update timestamp (nullable) |

---

### `ApiErrorResponse`

Returned by all endpoints on error conditions.

```json
{
  "timestamp":     "2026-03-11T10:00:00",
  "status":        404,
  "error":         "Not Found",
  "message":       "Event not found with id: 550e8400-...",
  "path":          "/api/v1/events/550e8400-...",
  "correlationId": "3f2504e0-4f89-11d3-9a0c-0305e82c3301"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `timestamp` | LocalDateTime | When the error occurred |
| `status` | int | HTTP status code |
| `error` | String | HTTP status reason phrase |
| `message` | String | Human-readable error description |
| `path` | String | Request URI that triggered the error |
| `correlationId` | String | Value of the `X-Correlation-Id` header on the request |

---

## Database Schema

Schema is managed by **Flyway** migrations.

**V1** — creates the `events` table:

```sql
CREATE TABLE IF NOT EXISTS events (
    id          VARCHAR(36)    NOT NULL,
    title       VARCHAR(150)   NOT NULL,
    description VARCHAR(500)   NOT NULL,
    date        VARCHAR(255)   NOT NULL,
    location    VARCHAR(200)   NOT NULL,
    base_price  DECIMAL(12, 2) NOT NULL,
    deleted     BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at  DATETIME       NOT NULL,
    updated_at  DATETIME,
    PRIMARY KEY (id)
);
```

**V2** — extends the `id` column to accommodate UUIDs:

```sql
ALTER TABLE events MODIFY COLUMN id VARCHAR(36) NOT NULL;
```

| Column | Type | Notes |
|--------|------|-------|
| `id` | VARCHAR(36) | Server-generated UUID primary key |
| `title` | VARCHAR(150) | Event name |
| `description` | VARCHAR(500) | Short summary |
| `date` | VARCHAR(255) | Date/time as a string |
| `location` | VARCHAR(200) | Venue |
| `base_price` | DECIMAL(12,2) | Reference price |
| `deleted` | BOOLEAN | Soft-delete flag (default `false`) |
| `created_at` | DATETIME | Set by JPA auditing on insert |
| `updated_at` | DATETIME | Set by JPA auditing on update (nullable) |

---

## Configuration

Key properties from `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: event-service

  # Config Server (optional — service starts without it)
  config:
    import: "optional:configserver:http://localhost:8088"

  datasource:
    url: jdbc:mysql://localhost:3306/ticketflow_events
          ?createDatabaseIfNotExist=true&useSSL=false
          &serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: validate      # Flyway owns the schema — Hibernate only validates
    show-sql: true

management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics
  endpoint:
    health:
      show-details: always
```

> The datasource URL includes `createDatabaseIfNotExist=true`, so MySQL will automatically create the `ticketflow_events` database if it does not exist.

---

## Running the Service

### Prerequisites

- Java 21
- Maven 3.9+
- MySQL 8 running locally (default: `localhost:3306`)

### Steps

1. **Clone the repository** and navigate to the service directory:

   ```bash
   git clone <repo-url>
   cd event-service
   ```

2. **Configure the database** — update credentials in `application.yml` if your MySQL user/password differs from the defaults (`root`/`root`). The database `ticketflow_events` is created automatically on first run.

3. **Run the service**:

   ```bash
   ./mvnw spring-boot:run
   ```

   The service starts on the port configured in the Config Server (or the default port if the Config Server is not available — the import is `optional:`).

4. *(Optional)* Start the **Config Server** and **Eureka Server** first for full service-discovery functionality.

---

## Running Tests

```bash
./mvnw test
```

Tests use an **H2 in-memory database** — no external MySQL instance is required. Flyway is disabled in the test profile to allow Hibernate to manage the schema against H2.

The test suite includes:
- **Unit tests** — `EventServiceTest`, `EventControllerTest`, `EventPersistenceAdapterTest`, `GlobalExceptionHandlerTest` (JUnit 5 + Mockito)
- **Integration tests** — `EventIntegrationTest` (`@SpringBootTest` + `@AutoConfigureMockMvc` + H2, full CRUD lifecycle)

---

## Health & Monitoring

Spring Boot Actuator exposes the following endpoints:

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/health` | Service health status and details |
| `GET /actuator/info` | Application info |
| `GET /actuator/metrics` | JVM and application metrics |
