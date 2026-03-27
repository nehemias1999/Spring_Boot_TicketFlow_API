# ticket-service

![Java 21](https://img.shields.io/badge/Java-21-blue)
![Spring Boot 3.5.4](https://img.shields.io/badge/Spring%20Boot-3.5.4-brightgreen)
![Spring Cloud 2025.0.0](https://img.shields.io/badge/Spring%20Cloud-2025.0.0-brightgreen)

Microservice responsible for managing ticket purchases in the **TicketFlow** ticket reservation system. Exposes a REST API consumed by the API Gateway to create, read, update, cancel, and soft-delete tickets.

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

`ticket-service` manages ticket bookings for the TicketFlow platform. It provides full CRUD operations with soft-delete support, a dedicated cancel endpoint, paginated and filterable listings, and automatic schema migrations via Flyway. Ticket IDs are server-generated UUIDs. The service registers itself with Eureka and is accessible through the API Gateway.

---

## Architecture

The service follows **Hexagonal Architecture (Ports & Adapters)** combined with **Vertical Slicing**, keeping domain logic isolated from infrastructure concerns.

```
┌──────────────────────────────────────────────────────────┐
│                    Inbound Adapter                        │
│         REST Controller  (TicketController)               │
│         /api/v1/tickets                                   │
└────────────────────────┬─────────────────────────────────┘
                         │ uses port in
┌────────────────────────▼─────────────────────────────────┐
│                   Application Layer                       │
│   TicketService  │  DTOs (Create/Update/Response)         │
│   MapStruct Mapper  │  Jakarta Validation                 │
└────────────────────────┬─────────────────────────────────┘
                         │ uses port out
┌────────────────────────▼─────────────────────────────────┐
│                     Domain Layer                          │
│   Ticket model  │  TicketStatus enum                      │
│   ITicketService (port in)                                │
│   ITicketPersistencePort (port out)  │  Exceptions        │
└────────────────────────┬─────────────────────────────────┘
                         │ implements port out
┌────────────────────────▼─────────────────────────────────┐
│                   Outbound Adapter                        │
│   TicketPersistenceAdapter  │  TicketEntity               │
│   ITicketJpaRepository (Spring Data JPA)                  │
│   TicketSpecification (dynamic filters)                   │
│   Flyway migrations  │  MySQL 8                           │
└──────────────────────────────────────────────────────────┘
```

### Package overview

| Package | Responsibility |
|---------|---------------|
| `booking.infrastructure.adapter.in.web` | REST controllers — inbound adapters |
| `booking.application.service` | Business logic — orchestrates domain operations |
| `booking.application.dto` | Request/response DTOs and MapStruct mappers |
| `booking.domain.model` | Core domain model (`Ticket`, `TicketStatus`) |
| `booking.domain.port.in` | Inbound port interfaces (`ITicketService`) |
| `booking.domain.port.out` | Outbound port interfaces (`ITicketPersistencePort`) |
| `booking.domain.exception` | Domain exceptions (`TicketNotFoundException`, `TicketAlreadyCancelledException`) |
| `booking.infrastructure.adapter.out.persistence` | JPA entities, repositories, persistence adapter |
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
| `POST` | `/api/v1/tickets` | Purchase a new ticket | `CreateTicketRequest` | `201 TicketResponse` |
| `GET` | `/api/v1/tickets/{id}` | Get ticket by ID | — | `200 TicketResponse` |
| `GET` | `/api/v1/tickets` | List tickets (paginated + filtered) | — | `200 Page<TicketResponse>` |
| `PUT` | `/api/v1/tickets/{id}` | Transfer ticket (update userId) | `UpdateTicketRequest` | `200 TicketResponse` |
| `PATCH` | `/api/v1/tickets/{id}/cancel` | Cancel a ticket | — | `200 TicketResponse` |
| `DELETE` | `/api/v1/tickets/{id}` | Soft-delete a ticket | — | `204 No Content` |

Interactive documentation is available via Swagger UI at `http://localhost:8082/swagger-ui.html`.

---

### POST `/api/v1/tickets`

Purchases a new ticket. Sets `purchaseDate` to now and `status` to `CONFIRMED`. The ID is generated server-side as a UUID.

- **201 Created** — ticket created successfully, returns `TicketResponse`
- **400 Bad Request** — validation failure

---

### GET `/api/v1/tickets/{id}`

Retrieves a single active ticket by its UUID.

- **200 OK** — returns `TicketResponse`
- **404 Not Found** — no active ticket with that ID

---

### GET `/api/v1/tickets`

Returns a paginated list of active tickets with optional filters. Page size is capped at 100.

| Query Parameter | Type | Default | Description |
|-----------------|------|---------|-------------|
| `page` | int | `0` | Page number (zero-based) |
| `size` | int | `10` | Items per page (max 100) |
| `eventId` | String | — | Filter by event ID (exact match) |
| `userId` | String | — | Filter by user ID (exact match) |
| `status` | String | — | Filter by status (`CONFIRMED` or `CANCELLED`) |
| `sortBy` | String | `createdAt` | Field to sort by |
| `sortDir` | String | `desc` | Sort direction: `asc` or `desc` |

- **200 OK** — returns `Page<TicketResponse>`

---

### PUT `/api/v1/tickets/{id}`

Transfers a ticket to a new owner (updates `userId`).

- **200 OK** — returns updated `TicketResponse`
- **404 Not Found** — ticket does not exist
- **400 Bad Request** — validation failure

---

### PATCH `/api/v1/tickets/{id}/cancel`

Cancels a confirmed ticket.

- **200 OK** — returns `TicketResponse` with `status: CANCELLED`
- **404 Not Found** — ticket does not exist
- **409 Conflict** — ticket is already cancelled

---

### DELETE `/api/v1/tickets/{id}`

Soft-deletes a ticket. The record is marked `deleted = true` and excluded from all active queries. The row is never physically removed.

- **204 No Content** — deleted successfully
- **404 Not Found** — ticket does not exist

---

## Request & Response Models

### `CreateTicketRequest`

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "userId":  "user-001"
}
```

| Field | Type | Constraints |
|-------|------|-------------|
| `eventId` | String | Required, max 36 characters |
| `userId` | String | Required, max 50 characters |

---

### `UpdateTicketRequest`

```json
{
  "userId": "user-002"
}
```

| Field | Type | Constraints |
|-------|------|-------------|
| `userId` | String | Required, max 50 characters |

---

### `TicketResponse`

**After purchase (`POST`)** — `updatedAt` is always `null`:

```json
{
  "id":           "3f2504e0-4f89-11d3-9a0c-0305e82c3301",
  "eventId":      "550e8400-e29b-41d4-a716-446655440000",
  "userId":       "user-001",
  "purchaseDate": "2026-03-11T10:00:00",
  "status":       "CONFIRMED",
  "createdAt":    "2026-03-11T10:00:00",
  "updatedAt":    null
}
```

**After transfer or cancel (`PUT` / `PATCH`)** — `updatedAt` is populated:

```json
{
  "id":           "3f2504e0-4f89-11d3-9a0c-0305e82c3301",
  "eventId":      "550e8400-e29b-41d4-a716-446655440000",
  "userId":       "user-002",
  "purchaseDate": "2026-03-11T10:00:00",
  "status":       "CONFIRMED",
  "createdAt":    "2026-03-11T10:00:00",
  "updatedAt":    "2026-03-11T12:00:00"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Server-generated UUID |
| `eventId` | String | Associated event ID |
| `userId` | String | Ticket owner |
| `purchaseDate` | LocalDateTime | When the ticket was purchased — format `yyyy-MM-dd'T'HH:mm:ss` |
| `status` | TicketStatus | `CONFIRMED` or `CANCELLED` |
| `createdAt` | LocalDateTime | Creation timestamp — format `yyyy-MM-dd'T'HH:mm:ss` |
| `updatedAt` | LocalDateTime | Last update timestamp — `null` on creation, populated after first update |

---

### `ApiErrorResponse`

Returned by all endpoints on error conditions.

```json
{
  "timestamp":     "2026-03-11T10:00:00",
  "status":        404,
  "error":         "Not Found",
  "message":       "Ticket with id '3f2504e0-...' not found",
  "path":          "/api/v1/tickets/3f2504e0-...",
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

**V1** — creates the `tickets` table:

```sql
CREATE TABLE IF NOT EXISTS tickets (
    id            VARCHAR(36)  NOT NULL,
    event_id      VARCHAR(36)  NOT NULL,
    user_id       VARCHAR(50)  NOT NULL,
    purchase_date DATETIME     NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    DATETIME     NOT NULL,
    updated_at    DATETIME,
    PRIMARY KEY (id)
);
```

**V2** — extends `id` and `event_id` columns to accommodate UUIDs:

```sql
ALTER TABLE tickets MODIFY COLUMN id       VARCHAR(36) NOT NULL;
ALTER TABLE tickets MODIFY COLUMN event_id VARCHAR(36) NOT NULL;
```

| Column | Type | Notes |
|--------|------|-------|
| `id` | VARCHAR(36) | Server-generated UUID primary key |
| `event_id` | VARCHAR(36) | Reference to an event |
| `user_id` | VARCHAR(50) | Reference to the purchasing user |
| `purchase_date` | DATETIME | Set on ticket creation |
| `status` | VARCHAR(20) | `CONFIRMED` or `CANCELLED` |
| `deleted` | BOOLEAN | Soft-delete flag (default `false`) |
| `created_at` | DATETIME | Set by JPA auditing on insert |
| `updated_at` | DATETIME | `NULL` on insert; set by JPA auditing on first update |

---

## API Conventions

### Datetime format

All `LocalDateTime` fields in responses use the format `yyyy-MM-dd'T'HH:mm:ss` (no nanoseconds):

```
2026-03-11T10:00:00
```

This is configured centrally in the config-server (`ticket-service.yml`):

```yaml
spring:
  jackson:
    serialization:
      write-dates-as-timestamps: false
      write-date-timestamps-as-nanoseconds: false
    date-format: yyyy-MM-dd'T'HH:mm:ss
```

### `updatedAt` field

- **On `POST` (creation):** `updatedAt` is always returned as `null`. The persistence adapter explicitly clears the in-memory value that Spring Auditing sets during the INSERT, so the response accurately reflects that no update has occurred yet.
- **On `PUT` / `PATCH` (modification):** `updatedAt` is populated by `@LastModifiedDate` (Spring Data JPA Auditing) with the timestamp of the update.

---

## Configuration

Key properties from `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: ticket-service

  # Config Server (optional — service starts without it)
  config:
    import: "optional:configserver:http://localhost:8088"

  datasource:
    url: jdbc:mysql://localhost:3306/ticketflow_tickets
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

> The datasource URL includes `createDatabaseIfNotExist=true`, so MySQL will automatically create the `ticketflow_tickets` database if it does not exist.

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
   cd ticket-service
   ```

2. **Configure the database** — update credentials in `application.yml` if your MySQL user/password differs from the defaults (`root`/`root`). The database `ticketflow_tickets` is created automatically on first run.

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
- **Unit tests** — `TicketServiceTest`, `TicketControllerTest`, `TicketPersistenceAdapterTest`, `GlobalExceptionHandlerTest` (JUnit 5 + Mockito)
- **Integration tests** — `TicketIntegrationTest` (`@SpringBootTest` + `@AutoConfigureMockMvc` + H2, full CRUD and cancel lifecycle)

---

## Health & Monitoring

Spring Boot Actuator exposes the following endpoints:

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/health` | Service health status and details |
| `GET /actuator/info` | Application info |
| `GET /actuator/metrics` | JVM and application metrics |
