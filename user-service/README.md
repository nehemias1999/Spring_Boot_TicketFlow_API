# user-service

![Java 21](https://img.shields.io/badge/Java-21-blue)
![Spring Boot 3.5.4](https://img.shields.io/badge/Spring%20Boot-3.5.4-brightgreen)
![Spring Cloud 2025.0.0](https://img.shields.io/badge/Spring%20Cloud-2025.0.0-brightgreen)
![Spring Security](https://img.shields.io/badge/Spring%20Security-JWT-brightgreen)

Microservice responsible for **user registration and authentication** in the TicketFlow platform. Issues JWT tokens consumed by the API Gateway to authenticate all subsequent requests. Manages roles and seeds an admin account on startup.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Tech Stack](#tech-stack)
4. [API Endpoints](#api-endpoints)
5. [Request & Response Models](#request--response-models)
6. [JWT Token](#jwt-token)
7. [Roles](#roles)
8. [Admin Seeder](#admin-seeder)
9. [Database Schema](#database-schema)
10. [RabbitMQ Events](#rabbitmq-events)
11. [Configuration](#configuration)
12. [Running the Service](#running-the-service)
13. [Running Tests](#running-tests)
14. [Health & Monitoring](#health--monitoring)

---

## Overview

`user-service` is the single source of truth for user identity in the TicketFlow platform. It handles registration (with username and role), login, and JWT generation. After login, the client includes the JWT in the `Authorization: Bearer` header; the API Gateway validates it and propagates user identity (`X-User-Id`, `X-User-Email`, `X-User-Role`) to downstream services.

On successful registration the service publishes a `UserRegisteredEvent` to RabbitMQ **after the DB transaction commits**, so the `notification-service` can send a welcome email.

An admin user (`admin@ticketflow.com`, role `ADMIN`) is seeded automatically on the first startup.

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
│   AdminUserSeeder  (seeds admin on startup)               │
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
| `auth.infrastructure.config` | Spring Security, RabbitMQ configuration, `AdminUserSeeder` |

---

## Tech Stack

| Category | Technology |
|----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.4 |
| Cloud | Spring Cloud 2025.0.0 (Config, Eureka, LoadBalancer) |
| Security | Spring Security 6, jjwt 0.12.3 (HS256) |
| Persistence | Spring Data JPA, Hibernate, MySQL 8, Flyway, HikariCP |
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

All other routes (`/api/v1/events/**`, `/api/v1/tickets/**`) require a valid JWT enforced by the API Gateway.

---

### POST `/api/v1/auth/register`

Creates a new user account. The password is stored as a BCrypt hash. The role defaults to `USER`. A welcome email is sent asynchronously via RabbitMQ after the transaction commits.

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
  "username": "johndoe",
  "email":    "user@example.com",
  "password": "secret123"
}
```

| Field | Type | Constraints |
|-------|------|-------------|
| `username` | String | Required, 3–50 characters |
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

Tokens are signed with **HS256** using the shared secret in `JWT_SECRET`.

### Payload claims

| Claim | Value |
|-------|-------|
| `sub` | User UUID |
| `email` | User email address |
| `role` | `USER`, `SELLER`, `MODERATOR`, or `ADMIN` |
| `iat` | Issued-at timestamp |
| `exp` | Expiration (24h after issue by default) |

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

## Roles

Roles are stored in a dedicated `roles` table with foreign key constraints.

| Role | Description |
|------|-------------|
| `USER` | Default role assigned on registration. Can purchase and cancel tickets. |
| `SELLER` | Can create, update, and delete events. |
| `MODERATOR` | Reserved for future moderation features. |
| `ADMIN` | Full access. Seeded automatically on startup. |

---

## Admin Seeder

On startup, `AdminUserSeeder` (`ApplicationRunner`) checks whether `admin@ticketflow.com` exists. If not, it creates the account:

| Field | Value |
|-------|-------|
| Email | `admin@ticketflow.com` |
| Username | `ADMIN` |
| Password | `admin123` (BCrypt-hashed) |
| Role | `ADMIN` |

If the admin already exists the seeder does nothing (idempotent).

---

## Database Schema

Schema is managed by **Flyway** migrations (database: `ticketflow_users`).

| Migration | Description |
|-----------|-------------|
| V1 | Creates the `users` table |
| V2 | Adds `role_permissions` table |
| V3 | Adds `username VARCHAR(50) NOT NULL` to `users` |
| V4 | Creates `roles` table, seeds roles, adds FK from `users.role` and `role_permissions.role` |

**`users` table (after all migrations):**

| Column | Type | Notes |
|--------|------|-------|
| `id` | VARCHAR(36) | Server-generated UUID primary key |
| `username` | VARCHAR(50) | Display name |
| `email` | VARCHAR(255) | Unique login identifier |
| `password` | VARCHAR(255) | BCrypt hash |
| `role` | VARCHAR(20) | FK → `roles.name` (`USER` default) |
| `deleted` | BOOLEAN | Soft-delete flag (default `false`) |
| `created_at` | DATETIME | Set by JPA auditing on insert |
| `updated_at` | DATETIME | Set by JPA auditing on update |

**`roles` table:**

| Column | Type | Notes |
|--------|------|-------|
| `name` | VARCHAR(20) | Primary key (`USER`, `SELLER`, `MODERATOR`, `ADMIN`) |
| `description` | VARCHAR(200) | Human-readable description |

---

## RabbitMQ Events

On successful registration the service publishes a `UserRegisteredEvent` to the `ticketflow.events` Topic Exchange with routing key `user.registered`. Publishing occurs **after the DB transaction commits** via `TransactionSynchronizationManager`.

### `UserRegisteredEvent` payload

```json
{
  "userId": "3f2504e0-4f89-11d3-9a0c-0305e82c3301",
  "email":  "user@example.com"
}
```

The `notification-service` consumes this from `user.registered.queue` and sends a welcome email.

---

## Configuration

All sensitive values are injected via environment variables:

| Variable | Description |
|----------|-------------|
| `DB_URL` | JDBC URL for `ticketflow_users` database |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `RABBITMQ_HOST` / `PORT` / `USERNAME` / `PASSWORD` | RabbitMQ connection |
| `JWT_SECRET` | HS256 signing key (must match api-gateway) |
| `JWT_EXPIRATION_MS` | Token TTL in milliseconds (default `86400000` = 24h) |
| `EUREKA_URL` | Eureka server URL |

---

## Running the Service

### Prerequisites

- Java 21, Maven 3.9+
- MySQL 8 on port `3306`
- RabbitMQ on port `5672`
- `discovery-service` and `config-server` running (optional)

```bash
cd user-service
./mvnw spring-boot:run
```

The service starts on port `8084`. The admin user is created on first startup.

---

## Running Tests

```bash
./mvnw test
```

Tests use an **H2 in-memory database** — no external dependencies required. The test suite includes:

| Test class | Type | Description |
|------------|------|-------------|
| `UserServiceTest` | Unit | Registration and login logic with Mockito |
| `AdminUserSeederTest` | Unit | Seeder creates admin when absent; skips when present |
| `UserIntegrationTest` | Integration | Register + login lifecycle with `@SpringBootTest` + H2 |

---

## Health & Monitoring

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/health` | Service health status |
| `GET /actuator/info` | Application info |
