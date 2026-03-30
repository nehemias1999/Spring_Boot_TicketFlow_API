# api-gateway

![Java 21](https://img.shields.io/badge/Java-21-blue)
![Spring Boot 3.5.4](https://img.shields.io/badge/Spring%20Boot-3.5.4-brightgreen)
![Spring Cloud 2025.0.0](https://img.shields.io/badge/Spring%20Cloud-2025.0.0-brightgreen)

Single entry point for all client requests in the **TicketFlow** ticket reservation system. Handles JWT authentication, routing, load balancing, resilience, CORS, and correlation ID propagation across microservices.

---

## Table of Contents

1. [Overview](#overview)
2. [Role in the Architecture](#role-in-the-architecture)
3. [Tech Stack](#tech-stack)
4. [JWT Authentication](#jwt-authentication)
5. [Routing](#routing)
6. [Filters](#filters)
7. [Fallback](#fallback)
8. [Configuration](#configuration)
9. [Running the Service](#running-the-service)
10. [Running Tests](#running-tests)
11. [Health & Monitoring](#health--monitoring)

---

## Overview

`api-gateway` is a **Spring Cloud Gateway** service built on a reactive Netty server. It sits in front of all backend microservices and is the only component directly reachable by external clients. It resolves downstream service addresses dynamically via Eureka and fetches its route configuration from the config-server at startup.

---

## Role in the Architecture

```
                         Client
                           │
                           ▼
          ┌────────────────────────────────┐
          │           api-gateway           │
          │          localhost:8080          │
          │                                  │
          │  JwtAuthenticationFilter (global)│
          │  CorrelationIdFilter (global)    │
          │  CircuitBreaker + Retry (route)  │
          │  CORS (global)                   │
          └──────┬───────────┬──────┬────────┘
                 │           │      │
      lb://user  lb://event  lb://ticket
       -service   -service    -service
```

**Startup order** — the discovery-service and config-server must be running before the api-gateway starts, since the gateway registers with Eureka and fetches its routes from the config-server.

---

## Tech Stack

| Category | Technology |
|----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.4 |
| Cloud | Spring Cloud 2025.0.0 (Gateway, Config, Eureka, LoadBalancer) |
| Security | jjwt 0.12.3 (JWT validation) |
| Resilience | Resilience4j (CircuitBreaker via Spring Cloud CircuitBreaker) |
| Monitoring | Spring Boot Actuator |
| Testing | JUnit 5 |
| Build | Maven |

---

## JWT Authentication

A global `JwtAuthenticationFilter` validates Bearer tokens on every request before routing.

**Public routes (no token required):**
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`

**All other routes** require a valid JWT in the `Authorization: Bearer <token>` header.

On a valid token, the filter extracts claims and adds headers for downstream services:

| Header forwarded | JWT Claim | Example |
|-----------------|-----------|---------|
| `X-User-Id` | `sub` | `3f2504e0-4f89-...` |
| `X-User-Email` | `email` | `user@example.com` |
| `X-User-Role` | `role` | `USER`, `SELLER`, `ADMIN` |

The JWT secret is configured via the `JWT_SECRET` environment variable (shared with user-service).

---

## Routing

Routes are defined in `config-server/src/main/resources/config/api-gateway.yml` and loaded at startup.

| Route ID | URI | Predicate | Description |
|----------|-----|-----------|-------------|
| `user-service` | `lb://user-service` | `Path=/api/v1/auth/**` | Auth endpoints (register, login) |
| `event-service` | `lb://event-service` | `Path=/api/v1/events/**` | Event catalog API |
| `ticket-service` | `lb://ticket-service` | `Path=/api/v1/tickets/**` | Ticket booking API |

---

## Filters

### Global — `JwtAuthenticationFilter`

- Validates the `Authorization: Bearer` token on every non-public request
- Rejects invalid/expired tokens with `401 Unauthorized`
- Extracts `sub`, `email`, `role` claims and forwards them as `X-User-*` headers

### Global — `CorrelationIdFilter`

- Reads `X-Correlation-Id` from the incoming request
- Generates a new UUID if absent
- Propagates the header downstream and adds it to the outgoing response
- Logs `[correlationId] METHOD /path` for every request

### Per-route — CircuitBreaker

Both business routes are wrapped with independent Resilience4j circuit breakers:

| Property | Value |
|----------|-------|
| Sliding window size | 10 calls |
| Failure rate threshold | 50% |
| Wait duration in open state | 10s |
| Calls permitted in half-open state | 3 |
| Auto transition to half-open | enabled |

When a circuit opens, requests are forwarded to the fallback endpoint (`/fallback/events` or `/fallback/tickets`).

### Per-route — Retry

| Property | Value |
|----------|-------|
| Retries | 3 |
| Retried statuses | `BAD_GATEWAY`, `SERVICE_UNAVAILABLE` |
| Methods | `GET` only |
| First backoff | 100ms |
| Max backoff | 500ms |
| Backoff factor | 2× |

### Global — CORS

| Property | Value |
|----------|-------|
| Allowed origins | Configured via `CORS_ALLOWED_ORIGINS` env var (default `http://localhost:3000`) |
| Allowed methods | `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS` |
| Allowed headers | `Authorization`, `Content-Type`, `X-Correlation-Id`, `X-Requested-With` |
| Allow credentials | `true` |

---

## Fallback

When a circuit breaker is open, the gateway forwards to the `FallbackController`, which returns a structured `503 Service Unavailable` response:

```json
{
  "timestamp": "2026-03-11T10:00:00",
  "status":    503,
  "error":     "Service Unavailable",
  "message":   "The event service is temporarily unavailable. Please try again later.",
  "path":      "/fallback/events"
}
```

---

## Configuration

Key environment variables:

| Variable | Description |
|----------|-------------|
| `JWT_SECRET` | HS256 signing key — must match user-service |
| `JWT_EXPIRATION_MS` | Token TTL validation window |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins (default `http://localhost:3000`) |
| `EUREKA_URL` | Eureka server URL |
| `ZIPKIN_ENDPOINT` | Distributed tracing endpoint |

The gateway's `application.yml` only declares the application name and config-server import. All routing, resilience, CORS, and JWT settings are served by the config-server (`api-gateway.yml`).

---

## Running the Service

### Prerequisites

- Java 21, Maven 3.9+
- `discovery-service` running at `localhost:8761`
- `config-server` running at `localhost:8088`

```bash
cd api-gateway
./mvnw spring-boot:run
```

> The config-server import is declared as `optional:`, so the gateway will still start if the config-server is unavailable — but it will have no routes configured.

---

## Running Tests

```bash
./mvnw test
```

The test profile disables the config-server import and Eureka client so the context loads without external services.

---

## Health & Monitoring

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/health` | Service health and circuit breaker state |
| `GET /actuator/info` | Application info |
