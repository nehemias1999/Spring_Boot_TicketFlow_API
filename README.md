# TicketFlow API

![Java 21](https://img.shields.io/badge/Java-21-blue)
![Spring Boot 3.5.4](https://img.shields.io/badge/Spring%20Boot-3.5.4-brightgreen)
![Spring Cloud 2025.0.0](https://img.shields.io/badge/Spring%20Cloud-2025.0.0-brightgreen)
![Maven](https://img.shields.io/badge/Build-Maven-orange)

Microservices-based ticket reservation system built with Java 21 and Spring Boot. The project demonstrates production-oriented patterns including JWT authentication, service discovery, centralized configuration, API gateway routing, resilience, event-driven notifications, and hexagonal architecture.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Services](#services)
4. [Tech Stack](#tech-stack)
5. [Authentication](#authentication)
6. [API Conventions](#api-conventions)
7. [Port Reference](#port-reference)
8. [Startup Order](#startup-order)
9. [Running the Project](#running-the-project)
10. [Running All Tests](#running-all-tests)

---

## Overview

TicketFlow is an incremental Spring Boot microservices project. Each service is independently deployable, registers itself with a central Eureka server, and fetches its configuration from a centralized config server at startup. External clients interact exclusively through the API Gateway, which validates JWT tokens and propagates user identity to downstream services via trusted headers.

---

## Architecture

```
                            Client
                              │
                              ▼
             ┌────────────────────────────────┐
             │           api-gateway           │
             │           port 8080             │
             │  JWT auth · routing · resilience│
             └──────┬──────────┬──────┬────────┘
                    │          │      │
          lb://user lb://event lb://ticket
           -service  -service   -service
                    │          │      │
                    ▼          ▼      ▼
          ┌──────────────┐ ┌──────────────┐ ┌──────────────────┐
          │ user-service │ │ event-service│ │  ticket-service  │
          │  port 8084   │ │  port 8081   │ │   port 8082      │
          │ auth · JWT   │ │ CRUD events  │ │ CRUD tickets     │
          │ MySQL·Flyway │ │ MySQL·Flyway │ │ MySQL · Flyway   │
          └──────────────┘ └──────────────┘ └────────┬─────────┘
                │                                     │ publishes
                │ publishes                           │
                ▼                                     ▼
         ┌──────────────────────────────────────────────────┐
         │                    RabbitMQ                       │
         │           ticketflow.events (topic exchange)      │
         │  user.registered  ·  ticket.purchased             │
         └───────────────────────┬──────────────────────────┘
                                 │ consumes
                                 ▼
                    ┌────────────────────────┐
                    │  notification-service  │
                    │      port 8083         │
                    │  email notifications   │
                    │  (event-driven, AMQP)  │
                    └────────────────────────┘

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
- **JWT authentication** — validates Bearer tokens on every request; public routes (`/api/v1/auth/**`) are exempt
- Extracts user identity from JWT and propagates `X-User-Id`, `X-User-Email`, `X-User-Role` headers to downstream services
- Routes requests to downstream services resolved via Eureka load balancer
- Global `X-Correlation-Id` propagation across all requests
- Circuit breaker (Resilience4j) and retry per route
- Global CORS policy
- Rate limiting (sliding window, 100 req/60s per IP)
- Structured fallback response when a downstream service is unavailable

→ [api-gateway/README.md](api-gateway/README.md)

---

### user-service
Business microservice that manages **user identity and authentication** for the TicketFlow platform. Responsibilities:
- User registration with BCrypt password hashing
- Login with JWT token generation (HS256, 24h expiry)
- Publishes `UserRegisteredEvent` to RabbitMQ on successful registration

→ [user-service/README.md](user-service/README.md)

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
- Ownership validation — only the purchasing user can modify or cancel their ticket (returns `403 Forbidden` otherwise)
- User identity read from `X-User-Id` / `X-User-Email` headers set by the gateway (no JWT parsing in this service)
- Paginated and filterable ticket listings
- Server-generated UUID identifiers
- Schema management via Flyway migrations
- Jakarta Bean Validation on all inputs
- Swagger UI at `/swagger-ui.html`
- Publishes `TicketPurchasedMessage` to RabbitMQ on every successful ticket creation

→ [ticket-service/README.md](ticket-service/README.md)

---

### notification-service
Event-driven microservice that delivers **email notifications** asynchronously. Does not expose HTTP endpoints — all input arrives via RabbitMQ. Responsibilities:
- Listens to `ticket.purchased.queue` → sends ticket confirmation email
- Listens to `user.registered.queue` → sends welcome email on new user registration
- Delivers email via JavaMailSender (SMTP — MailHog in development)

→ [notification-service/README.md](notification-service/README.md)

---

## Tech Stack

| Category | Technology |
|----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.4 |
| Cloud | Spring Cloud 2025.0.0 |
| Security | Spring Security, JWT (jjwt 0.12.3, HS256) |
| Service Discovery | Netflix Eureka |
| API Gateway | Spring Cloud Gateway (reactive Netty) |
| Config Management | Spring Cloud Config Server (native profile) |
| Load Balancing | Spring Cloud LoadBalancer |
| Resilience | Resilience4j (CircuitBreaker, Retry) |
| Messaging | RabbitMQ, Spring AMQP |
| Email | JavaMailSender, MailHog (development SMTP) |
| Persistence | Spring Data JPA, Hibernate, MySQL 8 |
| Migrations | Flyway |
| Mapping | MapStruct 1.6.3 |
| Validation | Jakarta Bean Validation |
| API Docs | springdoc-openapi 2.8.8 (Swagger UI) |
| Monitoring | Spring Boot Actuator |
| Testing | JUnit 5, Mockito, H2 (in-memory), spring-rabbit-test |
| Build | Maven |
| Utils | Lombok |

---

## Authentication

All endpoints except `/api/v1/auth/register` and `/api/v1/auth/login` require a valid JWT token.

### Register
```
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "secret"
}
```

### Login
```
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "secret"
}
```
Returns:
```json
{ "token": "<JWT>" }
```

### Authenticated requests
```
GET /api/v1/tickets
Authorization: Bearer <JWT>
```

The gateway validates the token, extracts the user's `sub` (userId), `email`, and `role` from the JWT claims, and forwards them to downstream services as:

| Header | Content |
|--------|---------|
| `X-User-Id` | User UUID |
| `X-User-Email` | User email address |
| `X-User-Role` | `USER` or `ADMIN` |

---

## API Conventions

### Datetime format

All `LocalDateTime` fields (`createdAt`, `updatedAt`, `purchaseDate`) are serialized in the format `yyyy-MM-dd'T'HH:mm:ss` — no milliseconds or nanoseconds:

```
2026-03-11T10:00:00
```

This is configured centrally via Jackson settings in the config-server YAML files for each service (`write-dates-as-timestamps: false`, `write-date-timestamps-as-nanoseconds: false`).

### `updatedAt` field

- **On creation (`POST`):** `updatedAt` is set to the same value as `createdAt` by Spring Auditing's `@LastModifiedDate` during the INSERT.
- **On modification (`PUT` / `PATCH`):** `updatedAt` is updated by `@LastModifiedDate` to the timestamp of the modification.

---

## Port Reference

| Service | Port |
|---------|------|
| discovery-service | `8761` |
| config-server | `8088` |
| api-gateway | `8080` |
| event-service | `8081` |
| ticket-service | `8082` |
| notification-service | `8083` |
| user-service | `8084` |
| RabbitMQ | `5672` (AMQP) · `15672` (management UI) |
| MailHog | `1025` (SMTP) · `8025` (web UI) |

---

## Startup Order

Services must be started in the following order due to registration and configuration dependencies:

```
1. discovery-service      ← Eureka must be up before anyone registers
2. config-server          ← must be up before clients fetch their config
3. RabbitMQ               ← must be up before services that use AMQP
   MailHog                ← must be up before notification-service sends email
4. event-service          ← registers with Eureka, fetches config
   ticket-service         ← registers with Eureka, fetches config (parallel with event-service)
   notification-service   ← registers with Eureka, connects to RabbitMQ (parallel)
   user-service           ← registers with Eureka, fetches config (parallel)
5. api-gateway            ← registers with Eureka, fetches routes from config
```

> Starting a service before its dependencies will not cause a fatal failure — Spring Cloud clients retry registration and config fetch in the background — but starting in order avoids noise in the logs.

---

## Running the Project

### Prerequisites

- Java 21
- Maven 3.9+
- MySQL 8 running locally on port `3306` (required by event-service, ticket-service, and user-service)
- RabbitMQ running locally on port `5672` (required by ticket-service, user-service, and notification-service)
- MailHog running locally on port `1025` (required by notification-service for email delivery)

> RabbitMQ and MailHog can be started with Docker Compose:
> ```bash
> docker-compose -f infra/docker-compose.yml up -d
> ```

### Steps

```bash
# 1. Clone the repository
git clone <repo-url>
cd Spring_Boot_TicketFlow_API

# 2. Start infrastructure (RabbitMQ + MailHog)
docker-compose -f infra/docker-compose.yml up -d

# 3. Start discovery-service
cd discovery-service && ./mvnw spring-boot:run

# 4. Start config-server (new terminal)
cd config-server && ./mvnw spring-boot:run

# 5. Start business services (each in a new terminal)
cd event-service        && ./mvnw spring-boot:run
cd ticket-service       && ./mvnw spring-boot:run
cd notification-service && ./mvnw spring-boot:run
cd user-service         && ./mvnw spring-boot:run

# 6. Start api-gateway (new terminal)
cd api-gateway && ./mvnw spring-boot:run
```

Once all services are up:

| URL | Description |
|-----|-------------|
| `http://localhost:8761` | Eureka dashboard — registered instances |
| `http://localhost:8080/api/v1/auth/register` | Register a new user |
| `http://localhost:8080/api/v1/auth/login` | Login and obtain JWT |
| `http://localhost:8080/api/v1/events` | Events API via the gateway |
| `http://localhost:8080/api/v1/tickets` | Tickets API via the gateway |
| `http://localhost:8081/swagger-ui.html` | Event-service Swagger UI |
| `http://localhost:8082/swagger-ui.html` | Ticket-service Swagger UI |
| `http://localhost:8084/swagger-ui.html` | User-service Swagger UI |
| `http://localhost:8025` | MailHog web UI — inspect sent emails |
| `http://localhost:15672` | RabbitMQ management UI (guest/guest) |
| `http://localhost:8080/actuator/health` | Gateway health |
| `http://localhost:8081/actuator/health` | Event-service health |
| `http://localhost:8082/actuator/health` | Ticket-service health |
| `http://localhost:8084/actuator/health` | User-service health |
| `http://localhost:8088/event-service/default` | Raw config served to event-service |

---

## Running All Tests

Each service has an isolated test configuration (H2 / no Eureka / no config-server) so tests run without any external dependencies.

```bash
# From each service directory
cd event-service         && ./mvnw test
cd ticket-service        && ./mvnw test
cd notification-service  && ./mvnw test
cd user-service          && ./mvnw test
cd api-gateway           && ./mvnw test
cd config-server         && ./mvnw test
cd discovery-service     && ./mvnw test
```
