# user-service

![Java 21](https://img.shields.io/badge/Java-21-blue)
![Spring Boot 3.5.4](https://img.shields.io/badge/Spring%20Boot-3.5.4-brightgreen)
![Spring Cloud 2025.0.0](https://img.shields.io/badge/Spring%20Cloud-2025.0.0-brightgreen)
![Spring Security](https://img.shields.io/badge/Spring%20Security-JWT-brightgreen)

Microservice responsible for **user registration and authentication** in the TicketFlow platform. Issues JWT tokens consumed by the API Gateway to authenticate all subsequent requests.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Tech Stack](#tech-stack)
4. [API Endpoints](#api-endpoints)
5. [Request & Response Models](#request--response-models)
6. [JWT Token](#jwt-token)
7. [Database Schema](#database-schema)
8. [RabbitMQ Events](#rabbitmq-events)
9. [Configuration](#configuration)
10. [Running the Service](#running-the-service)
11. [Running Tests](#running-tests)
12. [Health & Monitoring](#health--monitoring)

---

## Overview

`user-service` is the single source of truth for user identity in the TicketFlow platform. It handles registration, login, and JWT generation. After login, the client includes the JWT in the `Authorization: Bearer` header; the API Gateway validates it and propagates user identity (`X-User-Id`, `X-User-Email`, `X-User-Role`) to downstream services without them needing to parse the token themselves.

On successful registration the service also publishes a `UserRegisteredEvent` to RabbitMQ so the `notification-service` can send a welcome email.

---

## Architecture

The service follows **Hexagonal Architecture (Ports & Adapters)** combined with **Vertical Slicing**, consistent with the rest of the TicketFlow platform.

```
┌──────────────────────────────────────────────────────────┐
│                    Inbound Adapter                        │
│         REST Controller  (AuthController)                 │
│         POST /api/v1/auth/register                        │
│         POST /api/v1/auth/login                           │
└────────────────────────┬─────────────────────────────────┘
                         │ uses port in
┌────────────────────────▼─────────────────────────────────┐
│                   Application Layer                       │
│   UserService  │  DTOs (RegisterRequest, LoginRequest,    │
│                │        AuthResponse)                     │
│   JwtUtil  (token generation & validation)                │
└────────────────────────┬─────────────────────────────────┘
                         │ uses ports out
┌────────────────────────▼─────────────────────────────────┐
│                     Domain Layer                          │
│   User model  │  UserRole enum                            │
│   IUserService (port in)                                  │
│   IUserPersistencePort (port out)                         │
│   IUserEventPublisher (port out)                          │
│   Exceptions (UserAlreadyExistsException,                 │
│               InvalidCredentialsException)                │
└────────────────────────┬─────────────────────────────────┘
                         │ implements ports out
┌────────────────────────▼─────────────────────────────────┐
│                   Outbound Adapters                       │
│   UserPersistenceAdapter  │  UserEntity                   │
│   IUserJpaRepository (Spring Data JPA)                    │
│   Flyway migrations  │  MySQL 8                           │
│   RabbitMQUserEventPublisher  (publishes AMQP events)     │
└──────────────────────────────────────────────────────────┘
```

### Package overview

| Package | Responsibility |
|---------|---------------|
| `auth.infrastructure.adapter.in.web` | REST controllers — inbound adapters |
| `auth.application.service` | Business logic — registration, login, JWT |
| `auth.application.dto` | Request/response DTOs |
| `auth.application.util` | `JwtUtil` — token generation and claim extraction |
| `auth.domain.model` | Core domain model (`User`, `UserRole`) |
| `auth.domain.port.in` | Inbound port interface (`IUserService`) |
| `auth.domain.port.out` | Outbound port interfaces (`IUserPersistencePort`, `IUserEventPublisher`) |
| `auth.domain.exception` | Domain exceptions |
| `auth.infrastructure.adapter.out.persistence` | JPA entities, repositories, persistence adapter |
| `auth.infrastructure.adapter.out.messaging` | RabbitMQ event publisher |
| `auth.infrastructure.config` | Spring Security, RabbitMQ configuration |

---

## Tech Stack

| Category | Technology |
|----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.4 |
| Cloud | Spring Cloud 2025.0.0 (Config, Eureka, LoadBalancer) |
| Security | Spring Security 6, jjwt 0.12.3 (HS256) |
| Persistence | Spring Data JPA, Hibernate, MySQL 8, Flyway |
| Messaging | Spring AMQP, RabbitMQ |
| Monitoring | Spring Boot Actuator |
| Testing | JUnit 5, Mockito, H2 (in-memory), spring-rabbit-test |
| Build | Maven |
| Utils | Lombok |

---

## API Endpoints

| Method | Path | Description | Auth required |
|--------|------|-------------|--------------|
| `POST` | `/api/v1/auth/register` | Register a new user | No |
| `POST` | `/api/v1/auth/login` | Login and obtain a JWT | No |

All other routes to downstream services (`/api/v1/events/**`, `/api/v1/tickets/**`) require a valid JWT token in the `Authorization` header — enforced by the API Gateway.

---

### POST `/api/v1/auth/register`

Creates a new user account. The password is stored as a BCrypt hash. A welcome email is sent asynchronously via RabbitMQ.

- **201 Created** — user registered, returns `AuthResponse` with JWT
- **409 Conflict** — a user with that email already exists
- **400 Bad Request** — validation failure

---

### POST `/api/v1/auth/login`

Validates credentials and returns a JWT token.

- **200 OK** — returns `AuthResponse` with JWT
- **401 Unauthorized** — invalid email or password

---

## Request & Response Models

### `RegisterRequest`

```json
{
  "email":    "user@example.com",
  "password": "secret123"
}
```

| Field | Type | Constraints |
|-------|------|-------------|
| `email` | String | Required, valid email format |
| `password` | String | Required, minimum 6 characters |

---

### `LoginRequest`

```json
{
  "email":    "user@example.com",
  "password": "secret123"
}
```

| Field | Type | Constraints |
|-------|------|-------------|
| `email` | String | Required |
| `password` | String | Required |

---

### `AuthResponse`

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

| Field | Type | Description |
|-------|------|-------------|
| `token` | String | JWT Bearer token (HS256, 24h expiry) |

---

## JWT Token

Tokens are signed with **HS256** using the shared secret configured in `jwt.secret`.

### Payload claims

| Claim | Value |
|-------|-------|
| `sub` | User UUID |
| `email` | User email address |
| `role` | `USER` or `ADMIN` |
| `iat` | Issued-at timestamp |
| `exp` | Expiration timestamp (24h after issue) |

### Usage

```
GET /api/v1/tickets
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

The API Gateway extracts these claims and forwards them as headers:

| Header | JWT Claim |
|--------|-----------|
| `X-User-Id` | `sub` |
| `X-User-Email` | `email` |
| `X-User-Role` | `role` |

---

## Database Schema

Schema is managed by **Flyway** migrations (database: `ticketflow_users`).

**V1** — creates the `users` table:

```sql
CREATE TABLE IF NOT EXISTS users (
    id         VARCHAR(36)  NOT NULL,
    email      VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(20)  NOT NULL DEFAULT 'USER',
    deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME     NOT NULL,
    updated_at DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email)
);
```

| Column | Type | Notes |
|--------|------|-------|
| `id` | VARCHAR(36) | Server-generated UUID primary key |
| `email` | VARCHAR(255) | Unique user email (login identifier) |
| `password` | VARCHAR(255) | BCrypt hash |
| `role` | VARCHAR(20) | `USER` (default) or `ADMIN` |
| `deleted` | BOOLEAN | Soft-delete flag (default `false`) |
| `created_at` | DATETIME | Set by JPA auditing on insert |
| `updated_at` | DATETIME | Set by JPA auditing on update |

---

## RabbitMQ Events

On successful registration, the service publishes a `UserRegisteredEvent` to the `ticketflow.events` Topic Exchange with routing key `user.registered`.

### `UserRegisteredEvent` payload

```json
{
  "userId": "3f2504e0-4f89-11d3-9a0c-0305e82c3301",
  "email":  "user@example.com"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `userId` | String | UUID of the newly registered user |
| `email` | String | Email address for the welcome notification |

The `notification-service` consumes this from `user.registered.queue` and sends a welcome email.

---

## Configuration

Key properties from `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: user-service

  config:
    import: "optional:configserver:http://localhost:8088"

  datasource:
    url: jdbc:mysql://localhost:3306/ticketflow_users
          ?createDatabaseIfNotExist=true&useSSL=false
          &serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

  jpa:
    hibernate:
      ddl-auto: validate    # Flyway owns the schema
    show-sql: true

jwt:
  secret: ${JWT_SECRET:ticketflow-secret-key-must-be-at-least-32-chars-long!!}
  expiration-ms: ${JWT_EXPIRATION_MS:86400000}   # 24 hours

management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics
  endpoint:
    health:
      show-details: always
```

> The `jwt.secret` must match the value configured in the API Gateway. Both services read it from the Config Server (`user-service.yml` / `api-gateway.yml`).

---

## Running the Service

### Prerequisites

- Java 21
- Maven 3.9+
- MySQL 8 running locally on port `3306`
- RabbitMQ running locally on port `5672`

### Steps

1. **Clone the repository** and navigate to the service directory:

   ```bash
   git clone <repo-url>
   cd user-service
   ```

2. **Run the service**:

   ```bash
   ./mvnw spring-boot:run
   ```

   The service starts on port `8084` (configured in the Config Server).

3. *(Optional)* Start the **Config Server** and **Eureka Server** first for full service-discovery functionality.

---

## Running Tests

```bash
./mvnw test
```

Tests use an **H2 in-memory database** — no external MySQL instance is required. Flyway is disabled in the test profile to allow Hibernate to manage the schema against H2.

---

## Health & Monitoring

Spring Boot Actuator exposes the following endpoints:

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/health` | Service health status and details |
| `GET /actuator/info` | Application info |
| `GET /actuator/metrics` | JVM and application metrics |
