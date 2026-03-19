# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

TicketFlow is a microservices-based event ticketing API built with Java 21, Spring Boot 3.5.4, and Spring Cloud 2025.0.0.

## Services and Ports

| Service | Port | Role |
|---|---|---|
| `discovery-service` | 8761 | Netflix Eureka server |
| `config-server` | 8088 | Spring Cloud Config (native, serves configs from `classpath:/config/`) |
| `event-service` | 8081 | Event CRUD business service |
| `ticket-service` | 8082 | Ticket booking/transfer/cancellation business service |
| `api-gateway` | 8080 | Spring Cloud Gateway — routes, circuit breaker, retry, CORS, correlation ID |

## Build and Run Commands

Each service uses its own Maven wrapper. Run all commands from within the target service directory.

```bash
# Run a service
cd <service-dir> && ./mvnw spring-boot:run

# Build a JAR
cd <service-dir> && ./mvnw clean package

# Run all tests
cd <service-dir> && ./mvnw test

# Run a single test class
./mvnw test -Dtest=EventServiceTest

# Run a single test method
./mvnw test -Dtest=EventServiceTest#testCreateEventSuccess
```

**Required startup order:**
1. `discovery-service`
2. `config-server`
3. `event-service` and/or `ticket-service`
4. `api-gateway`

## Infrastructure Requirements

- MySQL running locally on port 3306 (credentials: `root`/`root`)
- Databases: `ticketflow_events` and `ticketflow_tickets` (auto-created by MySQL connector on first run)
- Schema managed by Flyway migrations in `src/main/resources/db/migration/`

## Architecture

### Hexagonal Architecture (event-service and ticket-service)

Both business services follow the same layered package structure:

```
com.ticketflow.<service>/
├── adapter/
│   ├── inbound/rest/         # REST controllers
│   └── outbound/persistence/ # JPA repositories and persistence adapters
├── application/
│   ├── dto/                  # Request/response DTOs
│   ├── mapper/               # MapStruct mappers
│   └── service/              # Application service (orchestration)
└── domain/
    ├── model/                # Domain entities
    ├── port/                 # Input/output port interfaces
    └── exception/            # Domain-specific exceptions
```

### API Gateway

- Routes all traffic via `lb://` (Eureka load-balanced URLs)
- Routes: `/api/v1/events/**` → event-service, `/api/v1/tickets/**` → ticket-service
- Adds `X-Correlation-Id` header to every request (generated if absent)
- Circuit breaker: 50% failure threshold, 10s wait; fallback controllers return `503`
- Retry: 3 attempts with exponential backoff (100ms–500ms), GET requests only
- Route configuration lives in `config-server/src/main/resources/config/api-gateway.yml`

### Config Server

Uses the `native` profile — all service configs are YAML files under `config-server/src/main/resources/config/`. Each business service fetches its config on startup from the config server.

## Testing

Tests use:
- JUnit 5 + Mockito for unit tests
- H2 in-memory database for persistence adapter tests
- `src/test/resources/application.properties` disables Eureka, Config Server import, and Flyway; uses H2 with `create-drop`

## Key Conventions

- **Soft-delete**: Records are never physically deleted. All entities have a `deleted` boolean column; queries filter `deleted = false`.
- **IDs**: String-based IDs (`VARCHAR(20)`) generated in the application layer.
- **Pagination and filtering**: List endpoints support `page`, `size`, `sortBy`, `sortDir`, plus entity-specific filter params via JPA Specifications.
- **Validation**: Jakarta Bean Validation annotations on DTOs; `@Valid` enforced in controllers.
- **MapStruct**: Used for all DTO ↔ domain model mapping; mappers are in the `application/mapper/` package.
