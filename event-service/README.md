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
7. [Security](#security)
8. [Configuration](#configuration)
9. [Running the Service](#running-the-service)
10. [Running Tests](#running-tests)
11. [Health & Monitoring](#health--monitoring)

---

## Overview

`event-service` manages the event catalog for the TicketFlow platform. It provides full CRUD operations with soft-delete support, paginated and filterable listings, capacity tracking, and automatic schema migrations via Flyway. Event IDs are server-generated UUIDs. The service registers itself with Eureka and is accessible through the API Gateway.

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
| `shared.infrastructure.filter` | `UserAuthHeaderFilter`, `SecurityHeadersFilter` |
| `shared.infrastructure.config` | JPA auditing configuration |

---

## Tech Stack

| Category | Technology |
|----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.4 |
| Cloud | Spring Cloud 2025.0.0 (Config, Eureka, LoadBalancer) |
| Persistence | Spring Data JPA, Hibernate, MySQL 8, Flyway, HikariCP |
| Mapping | MapStruct 1.6.3 |
| Validation | Jakarta Bean Validation |
| API Docs | springdoc-openapi 2.8.8 (Swagger UI) |
| Monitoring | Spring Boot Actuator |
| Testing | JUnit 5, Mockito, H2 (in-memory) |
| Build | Maven |
| Utils | Lombok |

---

## API Endpoints

| Method | Path | Description | Auth | Role required |
|--------|------|-------------|------|--------------|
| `POST` | `/api/v1/events` | Create a new event | Yes | `SELLER` or `ADMIN` |
| `GET` | `/api/v1/events/{id}` | Get event by ID | No | — |
| `GET` | `/api/v1/events` | List events (paginated + filtered) | No | — |
| `PUT` | `/api/v1/events/{id}` | Update an existing event | Yes | `SELLER` or `ADMIN` |
| `DELETE` | `/api/v1/events/{id}` | Soft-delete an event | Yes | `SELLER` or `ADMIN` |

Internal endpoints (called by ticket-service only):

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `PATCH` | `/api/v1/events/{id}/decrement-tickets` | Decrement available capacity | `X-Internal-Api-Key` |
| `PATCH` | `/api/v1/events/{id}/increment-tickets` | Increment available capacity (on cancel) | `X-Internal-Api-Key` |
| `GET` | `/api/v1/events/my-event-ids` | List event IDs created by the caller | `X-Internal-Api-Key` |

Interactive documentation is available via Swagger UI at `http://localhost:8081/swagger-ui.html`.

---

### POST `/api/v1/events`

Creates a new event. The ID is generated server-side as a UUID. Requires `X-User-Id` and `X-User-Role` headers (set by the API Gateway).

- **201 Created** — event created successfully, returns `EventResponse`
- **400 Bad Request** — validation failure
- **401 Unauthorized** — missing `X-User-Id` header
- **403 Forbidden** — role is not `SELLER` or `ADMIN`

---

### GET `/api/v1/events/{id}`

Retrieves a single active event by its UUID. No authentication required.

- **200 OK** — returns `EventResponse`
- **404 Not Found** — no active event with that ID

---

### GET `/api/v1/events`

Returns a paginated list of active events with optional filters. Page size is capped at 100. No authentication required.

| Query Parameter | Type | Default | Description |
|-----------------|------|---------|-------------|
| `page` | int | `0` | Page number (zero-based) |
| `size` | int | `10` | Items per page (max 100) |
| `title` | String | — | Filter by title (contains, case-insensitive) |
| `location` | String | — | Filter by location (contains, case-insensitive) |
| `sortBy` | String | `createdAt` | Field to sort by |
| `sortDir` | String | `desc` | Sort direction: `asc` or `desc` |

- **200 OK** — returns `Page<EventResponse>`
- **400 Bad Request** — `size` exceeds 100

---

### PUT `/api/v1/events/{id}`

Replaces all mutable fields of an existing event. Only the creator or an `ADMIN` can update an event.

- **200 OK** — returns updated `EventResponse`
- **403 Forbidden** — caller is not the event creator and is not `ADMIN`
- **404 Not Found** — event does not exist

---

### DELETE `/api/v1/events/{id}`

Soft-deletes an event. The record is marked `deleted = true` and excluded from all active queries.

- **204 No Content** — deleted successfully
- **403 Forbidden** — caller is not the event creator and is not `ADMIN`
- **404 Not Found** — event does not exist

---

## Request & Response Models

### `CreateEventRequest`

```json
{
  "title":       "Lollapalooza 2026",
  "description": "Annual music festival in Chicago",
  "date":        "2026-08-01T16:00:00",
  "location":    "Grant Park, Chicago, IL",
  "basePrice":   99.99,
  "capacity":    5000
}
```

| Field | Type | Constraints |
|-------|------|-------------|
| `title` | String | Required, 3–150 characters |
| `description` | String | Required, max 500 characters |
| `date` | LocalDateTime | Required |
| `location` | String | Required, max 200 characters |
| `basePrice` | BigDecimal | Required, ≥ 0 |
| `capacity` | int | Required, ≥ 1 |

---

### `UpdateEventRequest`

Same fields and constraints as `CreateEventRequest`.

---

### `EventResponse`

```json
{
  "id":                "550e8400-e29b-41d4-a716-446655440000",
  "title":             "Lollapalooza 2026",
  "description":       "Annual music festival in Chicago",
  "date":              "2026-08-01T16:00:00",
  "location":          "Grant Park, Chicago, IL",
  "basePrice":         99.99,
  "capacity":          5000,
  "availableTickets":  4999,
  "creatorId":         "user-001",
  "createdAt":         "2026-03-11T10:00:00",
  "updatedAt":         "2026-03-11T10:00:00"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Server-generated UUID |
| `title` | String | Event name |
| `description` | String | Short summary |
| `date` | LocalDateTime | Date and time of the event |
| `location` | String | Venue |
| `basePrice` | BigDecimal | Base ticket price |
| `capacity` | int | Total ticket capacity |
| `availableTickets` | int | Remaining tickets (`capacity` minus sold) |
| `creatorId` | String | ID of the user who created the event |
| `createdAt` | LocalDateTime | Creation timestamp |
| `updatedAt` | LocalDateTime | Last update timestamp |

---

### `ApiErrorResponse`

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

---

## Database Schema

Schema is managed by **Flyway** migrations.

| Migration | Description |
|-----------|-------------|
| V1 | Creates the `events` table |
| V2 | Extends `id` column to `VARCHAR(36)` |
| V3 | Adds `creator_id VARCHAR(50) NOT NULL` |
| V4 | Adds `capacity INT NOT NULL DEFAULT 0` |
| V5 | Adds `available_tickets INT NOT NULL DEFAULT 0` |
| V6 | Backfills `available_tickets` from `capacity` |
| V7 | Adds CHECK constraints and performance indexes |

**V7 constraints and indexes:**

```sql
-- Prevent overselling at the database level
ALTER TABLE events ADD CONSTRAINT chk_available_non_negative
    CHECK (available_tickets >= 0);
ALTER TABLE events ADD CONSTRAINT chk_available_lte_capacity
    CHECK (available_tickets <= capacity);
ALTER TABLE events ADD CONSTRAINT chk_capacity_positive
    CHECK (capacity >= 1);

-- Performance indexes
CREATE INDEX idx_events_creator_id      ON events (creator_id);
CREATE INDEX idx_events_creator_deleted ON events (creator_id, deleted);
CREATE INDEX idx_events_available       ON events (available_tickets);
```

---

## Security

### Role-based access control

Write operations (create, update, delete) require the `X-User-Role` header to contain `SELLER` or `ADMIN`. The `UserAuthHeaderFilter` enforces this before the request reaches the controller.

### Internal API key

The capacity endpoints (`/decrement-tickets`, `/increment-tickets`, `/my-event-ids`) are reserved for service-to-service calls from ticket-service. They require an `X-Internal-Api-Key` header matching the `INTERNAL_API_KEY` environment variable. Requests with a missing or incorrect key receive `401 Unauthorized`.

### Security response headers

Every response includes:

| Header | Value |
|--------|-------|
| `X-Content-Type-Options` | `nosniff` |
| `X-Frame-Options` | `DENY` |
| `Referrer-Policy` | `strict-origin-when-cross-origin` |
| `Cache-Control` | `no-store` |
| `Pragma` | `no-cache` |

---

## Configuration

All sensitive values are injected via environment variables. Key variables:

| Variable | Description |
|----------|-------------|
| `DB_URL` | JDBC URL for `ticketflow_events` database |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `INTERNAL_API_KEY` | Secret for service-to-service calls |
| `EUREKA_URL` | Eureka server URL |
| `SHOW_SQL` | Set to `true` to log SQL queries (default `false`) |
| `MANAGEMENT_ENDPOINTS_INCLUDE` | Actuator endpoints to expose |

HikariCP connection pool defaults (configurable via config-server):

| Setting | Default |
|---------|---------|
| `minimum-idle` | `5` |
| `maximum-pool-size` | `20` |
| `connection-timeout` | `30s` |
| `idle-timeout` | `10min` |
| `max-lifetime` | `30min` |

---

## Running the Service

### Option A — Docker Compose (recommended)

```bash
# From the repository root
cp .env.example .env   # fill in secrets on first run
docker-compose up -d
```

### Option B — Docker (standalone)

```bash
docker build -t ticketflow/event-service ./event-service
docker run -p 8081:8081 \
  -e DB_URL=jdbc:mysql://host.docker.internal:3306/ticketflow_events?createDatabaseIfNotExist=true\&useSSL=false\&serverTimezone=UTC\&allowPublicKeyRetrieval=true \
  -e DB_USERNAME=ticketflow \
  -e DB_PASSWORD=your-password \
  -e INTERNAL_API_KEY=your-internal-key \
  -e EUREKA_URL=http://host.docker.internal:8761/eureka/ \
  ticketflow/event-service
```

### Option C — Maven (local development)

#### Prerequisites

- Java 21, Maven 3.9+
- MySQL 8 on port `3306`
- `discovery-service` and `config-server` running (optional — import is `optional:`)

```bash
cd event-service
./mvnw spring-boot:run
```

---

## Running Tests

```bash
./mvnw test
```

Tests use an **H2 in-memory database** — no external MySQL instance required. The test suite includes:

| Test class | Type | Description |
|------------|------|-------------|
| `EventServiceTest` | Unit | Service layer business logic with Mockito |
| `EventControllerTest` | Unit | Controller layer with MockMvc |
| `EventPersistenceAdapterTest` | Unit | Persistence adapter with H2 |
| `GlobalExceptionHandlerTest` | Unit | Error response mapping |
| `UserAuthHeaderFilterTest` | Unit | Filter: auth rules for all endpoint types |
| `EventIntegrationTest` | Integration | Full CRUD lifecycle with `@SpringBootTest` + H2 |

---

## Health & Monitoring

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/health` | Service health status |
| `GET /actuator/info` | Application info |
