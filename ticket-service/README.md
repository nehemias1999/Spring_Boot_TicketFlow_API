# ticket-service

![Java 21](https://img.shields.io/badge/Java-21-blue)
![Spring Boot 3.5.4](https://img.shields.io/badge/Spring%20Boot-3.5.4-brightgreen)
![Spring Cloud 2025.0.0](https://img.shields.io/badge/Spring%20Cloud-2025.0.0-brightgreen)

Microservice responsible for managing ticket purchases in the **TicketFlow** ticket reservation system. Exposes a REST API consumed by the API Gateway to create, read, cancel, and soft-delete tickets. All write operations are protected by ownership validation — only the authenticated user who purchased a ticket can cancel or delete it.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Tech Stack](#tech-stack)
4. [API Endpoints](#api-endpoints)
5. [Request & Response Models](#request--response-models)
6. [Database Schema](#database-schema)
7. [Security](#security)
8. [RabbitMQ Events](#rabbitmq-events)
9. [Configuration](#configuration)
10. [Running the Service](#running-the-service)
11. [Running Tests](#running-tests)
12. [Health & Monitoring](#health--monitoring)

---

## Overview

`ticket-service` manages ticket bookings for the TicketFlow platform. It provides CRUD operations with soft-delete support, a dedicated cancel endpoint, paginated and filterable listings, and automatic schema migrations via Flyway. Ticket IDs are server-generated UUIDs. The service registers itself with Eureka and is accessible through the API Gateway.

User identity is never taken from the request body. The API Gateway validates the JWT token and propagates the user's ID, email, and role via `X-User-Id`, `X-User-Email`, and `X-User-Role` headers. The service trusts these headers and uses them to set the ticket owner and validate write access.

When a ticket is purchased the service calls the event-service internal API to decrement the event's available capacity. On cancellation the capacity is restored. RabbitMQ messages are published **after the DB transaction commits** to prevent message loss on rollback.

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
│   TicketService  │  DTOs (Create/Response)                │
│   MapStruct Mapper  │  Jakarta Validation                 │
└────────────────────────┬─────────────────────────────────┘
                         │ uses ports out
┌────────────────────────▼─────────────────────────────────┐
│                     Domain Layer                          │
│   Ticket model  │  TicketStatus enum                      │
│   ITicketService (port in)                                │
│   ITicketPersistencePort (port out)                       │
│   IEventServicePort (port out)                            │
│   ITicketEventPublisher (port out)                        │
│   Exceptions (TicketNotFoundException,                    │
│               TicketAlreadyCancelledException)             │
└────────────────────────┬─────────────────────────────────┘
                         │ implements ports out
┌────────────────────────▼─────────────────────────────────┐
│                   Outbound Adapters                       │
│   TicketPersistenceAdapter  │  TicketEntity               │
│   ITicketJpaRepository (Spring Data JPA)                  │
│   TicketSpecification (dynamic filters)                    │
│   EventServiceClient (HTTP → event-service internal API)  │
│   RabbitMQTicketEventPublisher (AMQP publishing)          │
│   Flyway migrations  │  MySQL 8                           │
└──────────────────────────────────────────────────────────┘
```

---

## Tech Stack

| Category | Technology |
|----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.4 |
| Cloud | Spring Cloud 2025.0.0 (Config, Eureka, LoadBalancer) |
| Persistence | Spring Data JPA, Hibernate, MySQL 8, Flyway, HikariCP |
| Messaging | Spring AMQP, RabbitMQ |
| Mapping | MapStruct 1.6.3 |
| Validation | Jakarta Bean Validation |
| API Docs | springdoc-openapi 2.8.8 (Swagger UI) |
| Monitoring | Spring Boot Actuator |
| Testing | JUnit 5, Mockito, H2 (in-memory) |
| Build | Maven |
| Utils | Lombok |

---

## API Endpoints

All endpoints require a valid JWT token (enforced by the API Gateway).

| Method | Path | Description | Response |
|--------|------|-------------|----------|
| `POST` | `/api/v1/tickets` | Purchase a new ticket | `201 TicketResponse` |
| `GET` | `/api/v1/tickets/{id}` | Get ticket by ID (own tickets only) | `200 TicketResponse` |
| `GET` | `/api/v1/tickets` | List own tickets (paginated + filtered) | `200 Page<TicketResponse>` |
| `PATCH` | `/api/v1/tickets/{id}/cancel` | Cancel a ticket | `200 TicketResponse` |
| `DELETE` | `/api/v1/tickets/{id}` | Soft-delete a ticket | `204 No Content` |

Interactive documentation is available via Swagger UI at `http://localhost:8082/swagger-ui.html`.

---

### POST `/api/v1/tickets`

Purchases a new ticket for the specified event. Sets `purchaseDate` to now and `status` to `CONFIRMED`. The ID is generated server-side. The `userId` is taken from the `X-User-Id` header — it is never part of the request body.

On success the service:
1. Decrements the event's `availableTickets` via the event-service internal API
2. Saves the ticket to the database
3. Publishes a `TicketPurchasedMessage` to RabbitMQ **after the DB transaction commits**

- **201 Created** — ticket created, returns `TicketResponse`
- **400 Bad Request** — validation failure or no capacity remaining
- **401 Unauthorized** — missing `X-User-Id` header

---

### GET `/api/v1/tickets/{id}`

Retrieves a single active ticket by its UUID. Only the ticket owner can view it.

- **200 OK** — returns `TicketResponse`
- **403 Forbidden** — ticket belongs to a different user
- **404 Not Found** — no active ticket with that ID

---

### GET `/api/v1/tickets`

Returns a paginated list of the authenticated user's tickets with optional filters. Page size is capped at 100.

| Query Parameter | Type | Default | Description |
|-----------------|------|---------|-------------|
| `page` | int | `0` | Page number (zero-based) |
| `size` | int | `10` | Items per page (max 100) |
| `eventId` | String | — | Filter by event ID (exact match) |
| `status` | String | — | Filter by status (`CONFIRMED` or `CANCELLED`) |
| `sortBy` | String | `createdAt` | Field to sort by |
| `sortDir` | String | `desc` | Sort direction: `asc` or `desc` |

- **200 OK** — returns `Page<TicketResponse>` (always scoped to the authenticated user)

---

### PATCH `/api/v1/tickets/{id}/cancel`

Cancels a confirmed ticket. Only the current owner can cancel. On success the service increments the event's `availableTickets` and publishes a `TicketCancelledMessage` after commit.

- **200 OK** — returns `TicketResponse` with `status: CANCELLED`
- **403 Forbidden** — authenticated user does not own the ticket
- **404 Not Found** — ticket does not exist
- **409 Conflict** — ticket is already cancelled

---

### DELETE `/api/v1/tickets/{id}`

Soft-deletes a ticket. The record is marked `deleted = true` and excluded from all active queries.

- **204 No Content** — deleted successfully
- **403 Forbidden** — authenticated user does not own the ticket
- **404 Not Found** — ticket does not exist

---

## Request & Response Models

### `CreateTicketRequest`

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000"
}
```

| Field | Type | Constraints |
|-------|------|-------------|
| `eventId` | String | Required, max 36 characters |

---

### `TicketResponse`

```json
{
  "id":           "3f2504e0-4f89-11d3-9a0c-0305e82c3301",
  "eventId":      "550e8400-e29b-41d4-a716-446655440000",
  "userId":       "user-001",
  "price":        99.99,
  "purchaseDate": "2026-03-11T10:00:00",
  "status":       "CONFIRMED",
  "createdAt":    "2026-03-11T10:00:00",
  "updatedAt":    "2026-03-11T10:00:00"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Server-generated UUID |
| `eventId` | String | Associated event ID |
| `userId` | String | Ticket owner |
| `price` | BigDecimal | Price at time of purchase (copied from event's `basePrice`) |
| `purchaseDate` | LocalDateTime | When the ticket was purchased |
| `status` | TicketStatus | `CONFIRMED` or `CANCELLED` |
| `createdAt` | LocalDateTime | Creation timestamp |
| `updatedAt` | LocalDateTime | Last update timestamp |

---

### `ApiErrorResponse`

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

---

## Database Schema

Schema is managed by **Flyway** migrations.

| Migration | Description |
|-----------|-------------|
| V1 | Creates the `tickets` table |
| V2 | Extends `id` and `event_id` to `VARCHAR(36)` |
| V3 | Adds `price DECIMAL(12,2) NOT NULL DEFAULT 0` |

**Current schema:**

| Column | Type | Notes |
|--------|------|-------|
| `id` | VARCHAR(36) | Server-generated UUID primary key |
| `event_id` | VARCHAR(36) | Reference to an event |
| `user_id` | VARCHAR(50) | Reference to the purchasing user |
| `price` | DECIMAL(12,2) | Price at purchase time |
| `purchase_date` | DATETIME | Set on ticket creation |
| `status` | VARCHAR(20) | `CONFIRMED` or `CANCELLED` |
| `deleted` | BOOLEAN | Soft-delete flag (default `false`) |
| `created_at` | DATETIME | Set by JPA auditing on insert |
| `updated_at` | DATETIME | Set by JPA auditing on insert and update |

---

## Security

### Ownership enforcement

All write endpoints (`PATCH`, `DELETE`) compare the `X-User-Id` header against the ticket's `userId`. A mismatch returns `403 Forbidden`. Listing and get-by-ID also enforce isolation — users can only see their own tickets.

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

## RabbitMQ Events

Messages are published to the `ticketflow.events` Topic Exchange. Publishing happens **after the DB transaction commits** via `TransactionSynchronizationManager` to prevent phantom messages on rollback.

| Event | Routing key | Trigger |
|-------|-------------|---------|
| `TicketPurchasedMessage` | `ticket.purchased` | Ticket successfully created |
| `TicketCancelledMessage` | `ticket.cancelled` | Ticket successfully cancelled |

### `TicketPurchasedMessage`

```json
{
  "ticketId":  "3f2504e0-4f89-11d3-9a0c-0305e82c3301",
  "userId":    "usr_abc123",
  "userEmail": "user@example.com"
}
```

---

## Configuration

All sensitive values are injected via environment variables:

| Variable | Description |
|----------|-------------|
| `DB_URL` | JDBC URL for `ticketflow_tickets` database |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `RABBITMQ_HOST` / `PORT` / `USERNAME` / `PASSWORD` | RabbitMQ connection |
| `INTERNAL_API_KEY` | Key sent to event-service internal endpoints |
| `EUREKA_URL` | Eureka server URL |
| `SHOW_SQL` | Log SQL queries (default `false`) |

HikariCP defaults: `minimum-idle: 5`, `maximum-pool-size: 20`, `connection-timeout: 30s`.

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
docker build -t ticketflow/ticket-service ./ticket-service
docker run -p 8082:8082 \
  -e DB_URL=jdbc:mysql://host.docker.internal:3306/ticketflow_tickets?createDatabaseIfNotExist=true\&useSSL=false\&serverTimezone=UTC\&allowPublicKeyRetrieval=true \
  -e DB_USERNAME=ticketflow \
  -e DB_PASSWORD=your-password \
  -e RABBITMQ_HOST=host.docker.internal \
  -e RABBITMQ_USERNAME=ticketflow \
  -e RABBITMQ_PASSWORD=your-password \
  -e INTERNAL_API_KEY=your-internal-key \
  -e EUREKA_URL=http://host.docker.internal:8761/eureka/ \
  ticketflow/ticket-service
```

### Option C — Maven (local development)

#### Prerequisites

- Java 21, Maven 3.9+
- MySQL 8 on port `3306`
- RabbitMQ on port `5672`
- `event-service` running (for capacity calls)
- `discovery-service` and `config-server` running (optional)

```bash
cd ticket-service
./mvnw spring-boot:run
```

---

## Running Tests

```bash
./mvnw test
```

Tests use an **H2 in-memory database** — no external dependencies required. The test suite includes:

| Test class | Type | Description |
|------------|------|-------------|
| `TicketServiceTest` | Unit | Service layer business logic with Mockito |
| `TicketControllerTest` | Unit | Controller layer with MockMvc |
| `TicketPersistenceAdapterTest` | Unit | Persistence adapter with H2 |
| `GlobalExceptionHandlerTest` | Unit | Error response mapping |
| `UserAuthHeaderFilterTest` | Unit | Filter: auth rules for all endpoint types |
| `TicketConcurrencyTest` | Concurrency | 10 threads buy same event; asserts no overselling |
| `TicketIntegrationTest` | Integration | Full CRUD and cancel lifecycle with `@SpringBootTest` + H2 |

---

## Health & Monitoring

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/health` | Service health status |
| `GET /actuator/info` | Application info |
