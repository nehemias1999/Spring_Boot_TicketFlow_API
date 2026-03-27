# TicketFlow API

![Java 21](https://img.shields.io/badge/Java-21-blue)
![Spring Boot 3.5.4](https://img.shields.io/badge/Spring%20Boot-3.5.4-brightgreen)
![Spring Cloud 2025.0.0](https://img.shields.io/badge/Spring%20Cloud-2025.0.0-brightgreen)
![Maven](https://img.shields.io/badge/Build-Maven-orange)

Microservices-based ticket reservation system built with Java 21 and Spring Boot. The project demonstrates production-oriented patterns including service discovery, centralized configuration, API gateway routing, resilience, and hexagonal architecture.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Services](#services)
4. [Tech Stack](#tech-stack)
5. [API Conventions](#api-conventions)
6. [Port Reference](#port-reference)
7. [Startup Order](#startup-order)
8. [Running the Project](#running-the-project)
9. [Running All Tests](#running-all-tests)

---

## Overview

TicketFlow is an incremental Spring Boot microservices project. Each service is independently deployable, registers itself with a central Eureka server, and fetches its configuration from a centralized config server at startup. External clients interact exclusively through the API Gateway.

---

## Architecture

```
                            Client
                              │
                              ▼
             ┌────────────────────────────────┐
             │           api-gateway           │
             │           port 8080             │
             │  routing · resilience · CORS    │
             └──────────┬───────────┬──────────┘
                        │           │
              lb://event-service   lb://ticket-service
                        │           │
                        ▼           ▼
             ┌──────────────┐  ┌──────────────────┐
             │ event-service│  │  ticket-service  │
             │  port 8081   │  │   port 8082      │
             │ CRUD events  │  │ CRUD tickets     │
             │ MySQL·Flyway │  │ MySQL · Flyway   │
             └──────────────┘  └──────────────────┘

     ┌──────────────────────────────────────────────┐
     │              discovery-service                │
     │    (Eureka Server)      port 8761             │
     │  all services register and resolve here       │
     └──────────────────────────────────────────────┘

     ┌──────────────────────────────────────────────┐
     │               config-server                   │
     │    (Spring Cloud Config)    port 8088         │
     │  serves yml config files to all services      │
     └──────────────────────────────────────────────┘
```

All business services follow **Hexagonal Architecture (Ports & Adapters)** combined with **Vertical Slicing**, keeping domain logic isolated from infrastructure concerns.

---

## Services

### discovery-service
Central **Netflix Eureka Server**. Every other microservice registers with it on startup and resolves the addresses of its dependencies through it at runtime. Must be the first service started.

→ [discovery-service/README.md](discovery-service/README.md)

---

### config-server
**Spring Cloud Config Server** running in `native` profile. Serves externalized YAML configuration files from `src/main/resources/config/` to all microservices at startup. Eliminates the need to hardcode environment-specific values inside each service.

→ [config-server/README.md](config-server/README.md)

---

### api-gateway
**Spring Cloud Gateway** acting as the single entry point for all client requests. Responsibilities:
- Routes requests to downstream services resolved via Eureka load balancer
- Global `X-Correlation-Id` propagation across all requests
- Circuit breaker (Resilience4j) and retry per route
- Global CORS policy
- Structured fallback response when a downstream service is unavailable

→ [api-gateway/README.md](api-gateway/README.md)

---

### event-service
Business microservice that manages the **event catalog** for the TicketFlow platform. Exposes a REST API consumed by the API Gateway. Responsibilities:
- Full CRUD with soft-delete support
- Paginated and filterable event listings
- Server-generated UUID identifiers
- Schema management via Flyway migrations
- Jakarta Bean Validation on all inputs
- Swagger UI at `/swagger-ui.html`

→ [event-service/README.md](event-service/README.md)

---

### ticket-service
Business microservice that manages **ticket purchases and transfers** for the TicketFlow platform. Exposes a REST API consumed by the API Gateway. Responsibilities:
- Full CRUD with soft-delete support and dedicated cancel endpoint
- Paginated and filterable ticket listings
- Server-generated UUID identifiers
- Schema management via Flyway migrations
- Jakarta Bean Validation on all inputs
- Swagger UI at `/swagger-ui.html`

→ [ticket-service/README.md](ticket-service/README.md)

---

## Tech Stack

| Category | Technology |
|----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.4 |
| Cloud | Spring Cloud 2025.0.0 |
| Service Discovery | Netflix Eureka |
| API Gateway | Spring Cloud Gateway (reactive Netty) |
| Config Management | Spring Cloud Config Server (native profile) |
| Load Balancing | Spring Cloud LoadBalancer |
| Resilience | Resilience4j (CircuitBreaker, Retry) |
| Persistence | Spring Data JPA, Hibernate, MySQL 8 |
| Migrations | Flyway |
| Mapping | MapStruct 1.6.3 |
| Validation | Jakarta Bean Validation |
| API Docs | springdoc-openapi 2.8.8 (Swagger UI) |
| Monitoring | Spring Boot Actuator |
| Testing | JUnit 5, Mockito, H2 (in-memory) |
| Build | Maven |
| Utils | Lombok |

---

## API Conventions

### Datetime format

All `LocalDateTime` fields (`createdAt`, `updatedAt`, `purchaseDate`) are serialized in the format `yyyy-MM-dd'T'HH:mm:ss` — no milliseconds or nanoseconds:

```
2026-03-11T10:00:00
```

This is configured centrally via Jackson settings in the config-server YAML files for each service (`write-dates-as-timestamps: false`, `write-date-timestamps-as-nanoseconds: false`).

### `updatedAt` field

- **On creation (`POST`):** `updatedAt` is returned as `null`. Spring Auditing's `@LastModifiedDate` sets the value in-memory during the INSERT, but the persistence adapter explicitly clears it before returning the response, so the result accurately reflects that no update has occurred yet.
- **On modification (`PUT` / `PATCH`):** `updatedAt` is populated by `@LastModifiedDate` with the actual update timestamp.

---

## Port Reference

| Service | Port |
|---------|------|
| discovery-service | `8761` |
| config-server | `8088` |
| api-gateway | `8080` |
| event-service | `8081` |
| ticket-service | `8082` |

---

## Startup Order

Services must be started in the following order due to registration and configuration dependencies:

```
1. discovery-service   ← Eureka must be up before anyone registers
2. config-server       ← must be up before clients fetch their config
3. event-service       ← registers with Eureka, fetches config
   ticket-service      ← registers with Eureka, fetches config (can start in parallel with event-service)
4. api-gateway         ← registers with Eureka, fetches routes from config
```

> Starting a service before its dependencies will not cause a fatal failure — Spring Cloud clients retry registration and config fetch in the background — but starting in order avoids noise in the logs.

---

## Running the Project

### Prerequisites

- Java 21
- Maven 3.9+
- MySQL 8 running locally on port `3306` (required by event-service and ticket-service)

### Steps

```bash
# 1. Clone the repository
git clone <repo-url>
cd Spring_Boot_TicketFlow_API

# 2. Start discovery-service
cd discovery-service && ./mvnw spring-boot:run

# 3. Start config-server (new terminal)
cd config-server && ./mvnw spring-boot:run

# 4. Start event-service (new terminal)
cd event-service && ./mvnw spring-boot:run

# 5. Start ticket-service (new terminal)
cd ticket-service && ./mvnw spring-boot:run

# 6. Start api-gateway (new terminal)
cd api-gateway && ./mvnw spring-boot:run
```

Once all services are up:

| URL | Description |
|-----|-------------|
| `http://localhost:8761` | Eureka dashboard — registered instances |
| `http://localhost:8080/api/v1/events` | Events API via the gateway |
| `http://localhost:8080/api/v1/tickets` | Tickets API via the gateway |
| `http://localhost:8081/swagger-ui.html` | Event-service Swagger UI |
| `http://localhost:8082/swagger-ui.html` | Ticket-service Swagger UI |
| `http://localhost:8080/actuator/health` | Gateway health |
| `http://localhost:8081/actuator/health` | Event-service health |
| `http://localhost:8082/actuator/health` | Ticket-service health |
| `http://localhost:8088/event-service/default` | Raw config served to event-service |

---

## Running All Tests

Each service has an isolated test configuration (H2 / no Eureka / no config-server) so tests run without any external dependencies.

```bash
# From each service directory
cd event-service  && ./mvnw test
cd ticket-service && ./mvnw test
cd api-gateway    && ./mvnw test
cd config-server  && ./mvnw test
cd discovery-service && ./mvnw test
```
