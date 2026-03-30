# TicketFlow API

![Java 21](https://img.shields.io/badge/Java-21-blue)
![Spring Boot 3.5.4](https://img.shields.io/badge/Spring%20Boot-3.5.4-brightgreen)
![Spring Cloud 2025.0.0](https://img.shields.io/badge/Spring%20Cloud-2025.0.0-brightgreen)
![Maven](https://img.shields.io/badge/Build-Maven-orange)

Microservices-based ticket reservation system built with Java 21 and Spring Boot. The project demonstrates production-oriented patterns including JWT authentication, role-based access control, service discovery, centralized configuration, API gateway routing, resilience, event-driven notifications, and hexagonal architecture.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Services](#services)
4. [Tech Stack](#tech-stack)
5. [Authentication & Roles](#authentication--roles)
6. [API Conventions](#api-conventions)
7. [Port Reference](#port-reference)
8. [Running with Docker Compose](#running-with-docker-compose)
9. [Running Locally](#running-locally)
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
         │  user.registered · ticket.purchased · ticket.cancelled
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
**Spring Cloud Config Server** running in `native` profile. Serves externalized YAML configuration files from `src/main/resources/config/` to all microservices at startup. All sensitive values (JWT secret, DB/RabbitMQ credentials, internal API key) are injected via environment variables.

→ [config-server/README.md](config-server/README.md)

---

### api-gateway
**Spring Cloud Gateway** acting as the single entry point for all client requests. Responsibilities:
- **JWT authentication** — validates Bearer tokens on every request; public routes (`/api/v1/auth/**`) are exempt
- Extracts user identity from JWT and propagates `X-User-Id`, `X-User-Email`, `X-User-Role` headers to downstream services
- Routes requests to downstream services resolved via Eureka load balancer
- Global `X-Correlation-Id` propagation across all requests
- Circuit breaker (Resilience4j) and retry per route
- Global CORS policy (origins controlled via `CORS_ALLOWED_ORIGINS` env var)
- Rate limiting (sliding window, 100 req/60s per IP)
- Structured fallback response when a downstream service is unavailable

→ [api-gateway/README.md](api-gateway/README.md)

---

### user-service
Business microservice that manages **user identity and authentication** for the TicketFlow platform. Responsibilities:
- User registration with username, BCrypt password hashing, and role assignment (`USER` by default)
- Login with JWT token generation (HS256, 24h expiry)
- Role-based access: `USER`, `SELLER`, `MODERATOR`, `ADMIN` — backed by a `roles` table with FK constraints
- Admin user seeded automatically on startup (`admin@ticketflow.com` / `ADMIN` role)
- Publishes `UserRegisteredEvent` to RabbitMQ after the DB transaction commits

→ [user-service/README.md](user-service/README.md)

---

### event-service
Business microservice that manages the **event catalog** for the TicketFlow platform. Responsibilities:
- Full CRUD with soft-delete; create/update/delete require `SELLER` or `ADMIN` role
- Capacity tracking (`capacity` and `availableTickets`) with DB-level CHECK constraints and indexes
- Internal endpoints (capacity decrement/increment) protected by `X-Internal-Api-Key` header
- Security response headers (`X-Content-Type-Options`, `X-Frame-Options`, `Cache-Control`, etc.)
- Paginated and filterable event listings (public, no auth required)
- Swagger UI at `/swagger-ui.html`

→ [event-service/README.md](event-service/README.md)

---

### ticket-service
Business microservice that manages **ticket purchases and cancellations** for the TicketFlow platform. Responsibilities:
- Ticket purchase with capacity enforcement (optimistic concurrency guard)
- Ownership validation — only the purchasing user can cancel or delete their ticket (`403 Forbidden` otherwise)
- Dedicated cancel endpoint (`PATCH /{id}/cancel`)
- User identity read from `X-User-Id` / `X-User-Email` / `X-User-Role` headers set by the gateway
- Paginated and filterable ticket listings (returns only the authenticated user's tickets)
- Security response headers
- Publishes `TicketPurchasedMessage` and `TicketCancelledMessage` to RabbitMQ after DB commit
- Swagger UI at `/swagger-ui.html`

→ [ticket-service/README.md](ticket-service/README.md)

---

### notification-service
Event-driven microservice that delivers **email notifications** asynchronously. Does not expose HTTP endpoints — all input arrives via RabbitMQ. Responsibilities:
- Listens to `ticket.purchased.queue` → sends ticket confirmation email
- Listens to `ticket.cancelled.queue` → sends ticket cancellation email
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
| Persistence | Spring Data JPA, Hibernate, MySQL 8, HikariCP |
| Migrations | Flyway |
| Mapping | MapStruct 1.6.3 |
| Validation | Jakarta Bean Validation |
| API Docs | springdoc-openapi 2.8.8 (Swagger UI) |
| Monitoring | Spring Boot Actuator, Zipkin (distributed tracing) |
| Testing | JUnit 5, Mockito, H2 (in-memory), spring-rabbit-test |
| Build | Maven |
| Utils | Lombok |
| Containers | Docker, Docker Compose |

---

## Authentication & Roles

All endpoints except `/api/v1/auth/register` and `/api/v1/auth/login` require a valid JWT token.

### Register
```
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "johndoe",
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
| `X-User-Role` | `USER`, `SELLER`, `MODERATOR`, or `ADMIN` |

### Roles

| Role | Description |
|------|-------------|
| `USER` | Default role. Can purchase and cancel own tickets. |
| `SELLER` | Can create, update, and delete events. |
| `MODERATOR` | Reserved for future moderation features. |
| `ADMIN` | Full access. Seeded automatically on startup (`admin@ticketflow.com`). |

---

## API Conventions

### Datetime format

All `LocalDateTime` fields are serialized as `yyyy-MM-dd'T'HH:mm:ss` (no milliseconds or nanoseconds):

```
2026-03-11T10:00:00
```

### `updatedAt` field

- **On creation (`POST`):** `updatedAt` is set to the same value as `createdAt` by Spring Auditing's `@LastModifiedDate` during the INSERT.
- **On modification (`PUT` / `PATCH`):** `updatedAt` is updated to the timestamp of the modification.

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
| MySQL | `3306` |
| MailHog | `1025` (SMTP) · `8025` (web UI) |
| Zipkin | `9411` |

---

## Running with Docker Compose

The easiest way to run the full stack is with Docker Compose. All services and infrastructure are defined in `docker-compose.yml` at the repository root.

### Prerequisites

- Docker Desktop (or Docker Engine + Compose plugin)
- A `.env` file in the repository root (copy from `.env.example`)

### 1. Configure environment variables

```bash
cp .env.example .env
```

Open `.env` and fill in the required values:

```bash
# Minimum required changes:
JWT_SECRET=your-very-strong-random-secret-at-least-32-chars
INTERNAL_API_KEY=your-strong-internal-api-key
DB_PASSWORD=your-db-password
RABBITMQ_PASSWORD=your-rabbitmq-password
```

> `MYSQL_ROOT_PASSWORD`, `CORS_ALLOWED_ORIGINS`, `MAIL_*`, and `MANAGEMENT_*` have sensible defaults in `.env.example` and can be left as-is for local development.

### 2. Build and start all services

```bash
docker-compose up -d
```

Docker Compose will:
1. Start infrastructure first (MySQL, RabbitMQ, Zipkin, MailHog) and wait for their healthchecks
2. Start `discovery-service` and `config-server`; wait for them to be healthy
3. Start all business services (`user-service`, `event-service`, `ticket-service`, `notification-service`) once config-server is healthy
4. Start `api-gateway` once `discovery-service` and `config-server` are healthy

### 3. Verify everything is up

```bash
docker-compose ps
```

All services should show `Up` or `Up (healthy)`.

### 4. Access the services

| URL | Description |
|-----|-------------|
| `http://localhost:8761` | Eureka dashboard — all registered instances |
| `http://localhost:8080/api/v1/auth/register` | Register a new user |
| `http://localhost:8080/api/v1/auth/login` | Login and obtain JWT |
| `http://localhost:8080/api/v1/events` | Events API via the gateway |
| `http://localhost:8080/api/v1/tickets` | Tickets API via the gateway |
| `http://localhost:8081/swagger-ui.html` | event-service Swagger UI |
| `http://localhost:8082/swagger-ui.html` | ticket-service Swagger UI |
| `http://localhost:8084/swagger-ui.html` | user-service Swagger UI |
| `http://localhost:8025` | MailHog — inspect sent emails |
| `http://localhost:15672` | RabbitMQ management UI |
| `http://localhost:9411` | Zipkin distributed tracing |
| `http://localhost:8080/actuator/health` | Gateway health |

### 5. Useful Docker Compose commands

```bash
# View logs for a specific service
docker-compose logs -f ticket-service

# Restart a single service (e.g. after a config change)
docker-compose restart event-service

# Stop all services (keeps volumes)
docker-compose down

# Stop all services and remove volumes (resets the database)
docker-compose down -v

# Rebuild a service image after code changes
docker-compose build ticket-service
docker-compose up -d ticket-service
```

### Admin account

An admin user is seeded automatically on the first startup of `user-service`:

| Field | Value |
|-------|-------|
| Email | `admin@ticketflow.com` |
| Password | `admin123` |
| Role | `ADMIN` |

---

## Running Locally

To run services without Docker (e.g. for development with hot reload), start infrastructure via Docker Compose and services with Maven.

### Prerequisites

- Java 21
- Maven 3.9+
- MySQL 8 on port `3306`
- RabbitMQ on port `5672`
- MailHog on port `1025`

Start infrastructure only:

```bash
# Start only the infrastructure services (MySQL, RabbitMQ, Zipkin, MailHog)
docker-compose up -d mysql rabbitmq zipkin mailhog
```

Then start services in order, each in a new terminal:

```bash
# 1. Discovery service
cd discovery-service && ./mvnw spring-boot:run

# 2. Config server
cd config-server && ./mvnw spring-boot:run

# 3. Business services (parallel)
cd event-service        && ./mvnw spring-boot:run
cd ticket-service       && ./mvnw spring-boot:run
cd notification-service && ./mvnw spring-boot:run
cd user-service         && ./mvnw spring-boot:run

# 4. API gateway (last)
cd api-gateway && ./mvnw spring-boot:run
```

> Starting a service before its dependencies will not cause a fatal failure — Spring Cloud clients retry registration and config fetch in the background — but starting in order avoids noise in the logs.

---

## Running All Tests

Each service has an isolated test configuration (H2 / no Eureka / no config-server) so tests run without any external dependencies.

```bash
cd event-service         && ./mvnw test
cd ticket-service        && ./mvnw test
cd notification-service  && ./mvnw test
cd user-service          && ./mvnw test
cd api-gateway           && ./mvnw test
cd config-server         && ./mvnw test
cd discovery-service     && ./mvnw test
```
