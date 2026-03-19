# api-gateway

![Java 21](https://img.shields.io/badge/Java-21-blue)
![Spring Boot 3.5.4](https://img.shields.io/badge/Spring%20Boot-3.5.4-brightgreen)
![Spring Cloud 2025.0.0](https://img.shields.io/badge/Spring%20Cloud-2025.0.0-brightgreen)

Single entry point for all client requests in the **TicketFlow** ticket reservation system. Handles routing, load balancing, resilience, CORS, and correlation ID propagation across microservices.

---

## Table of Contents

1. [Overview](#overview)
2. [Role in the Architecture](#role-in-the-architecture)
3. [Tech Stack](#tech-stack)
4. [Routing](#routing)
5. [Filters](#filters)
6. [Fallback](#fallback)
7. [Configuration](#configuration)
8. [Running the Service](#running-the-service)
9. [Running Tests](#running-tests)
10. [Health & Monitoring](#health--monitoring)

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
          │  CorrelationIdFilter (global)    │
          │  CircuitBreaker + Retry (route)  │
          │  CORS (global)                   │
          └──────────┬───────────┬───────────┘
                     │           │
           lb://event-service  lb://ticket-service
                     │           │
                     ▼           ▼
               event-service  ticket-service
               (port 8081)    (port 8082)
```

**Startup order** — the discovery-service and config-server must be running before the api-gateway starts, since the gateway registers with Eureka and fetches its routes from the config-server.

---

## Tech Stack

| Category | Technology |
|----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.4 |
| Cloud | Spring Cloud 2025.0.0 (Gateway, Config, Eureka, LoadBalancer) |
| Resilience | Resilience4j (CircuitBreaker via Spring Cloud CircuitBreaker) |
| Monitoring | Spring Boot Actuator |
| Testing | JUnit 5 |
| Build | Maven |

---

## Routing

Routes are defined in `config-server/src/main/resources/config/api-gateway.yml` and loaded at startup.

| Route ID | URI | Predicate | Description |
|----------|-----|-----------|-------------|
| `event-service` | `lb://event-service` | `Path=/api/v1/events/**` | Forwards all event API requests to event-service via Eureka |
| `ticket-service` | `lb://ticket-service` | `Path=/api/v1/tickets/**` | Forwards all ticket API requests to ticket-service via Eureka |

---

## Filters

### Global — `CorrelationIdFilter`

Applied to every request before it reaches any route filter or downstream service.

- Reads the `X-Correlation-Id` header from the incoming request.
- If absent or blank, generates a new UUID and injects it.
- Propagates the header downstream in the forwarded request.
- Adds the same header to the outgoing response so the client can trace the request end-to-end.
- Logs `[correlationId] METHOD /path` for every request.

### Per-route — CircuitBreaker

Both routes are wrapped with independent Resilience4j circuit breakers (`eventServiceCB` and `ticketServiceCB`) with identical configuration:

| Property | Value |
|----------|-------|
| Sliding window size | 10 calls |
| Failure rate threshold | 50% |
| Wait duration in open state | 10 s |
| Calls permitted in half-open state | 3 |
| Auto transition to half-open | enabled |

When a circuit is open, requests are forwarded to the corresponding fallback endpoint (`/fallback/events` or `/fallback/tickets`).

### Per-route — Retry

Automatically retries failed `GET` requests to both downstream services.

| Property | Value |
|----------|-------|
| Retries | 3 |
| Retried statuses | `BAD_GATEWAY`, `SERVICE_UNAVAILABLE` |
| Methods | `GET` only |
| First backoff | 100 ms |
| Max backoff | 500 ms |
| Backoff factor | 2× |

### Global — CORS

A global CORS policy is applied to all routes:

| Property | Value |
|----------|-------|
| Allowed origins | `*` |
| Allowed methods | `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS` |
| Allowed headers | `*` |

---

## Fallback

When a circuit breaker is open, the gateway forwards the request to the internal `FallbackController`, which returns a structured `503 Service Unavailable` response:

```json
{
  "timestamp": "2026-03-11T10:00:00",
  "status":    503,
  "error":     "Service Unavailable",
  "message":   "The event service is temporarily unavailable. Please try again later.",
  "path":      "/fallback/events"
}
```

The same pattern applies for `/fallback/tickets`.

---

## Configuration

The gateway's main `application.yml` only declares the application name and the optional config-server import. All routing, resilience, CORS, and Actuator settings are served by the config-server:

```yaml
# src/main/resources/application.yml
spring:
  application:
    name: api-gateway
  config:
    import: "optional:configserver:http://localhost:8088"
```

Key properties served by the config-server (`api-gateway.yml`):

```yaml
server:
  port: 8080

spring:
  cloud:
    gateway:
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins: "*"
            allowedMethods: [GET, POST, PUT, DELETE, OPTIONS]
            allowedHeaders: "*"
      server:
        webflux:
          routes:
            - id: event-service
              uri: lb://event-service
              predicates:
                - Path=/api/v1/events/**
              filters:
                - name: CircuitBreaker
                  args:
                    name: eventServiceCB
                    fallbackUri: forward:/fallback/events
                - name: Retry
                  args:
                    retries: 3
                    statuses: BAD_GATEWAY,SERVICE_UNAVAILABLE
                    methods: GET
            - id: ticket-service
              uri: lb://ticket-service
              predicates:
                - Path=/api/v1/tickets/**
              filters:
                - name: CircuitBreaker
                  args:
                    name: ticketServiceCB
                    fallbackUri: forward:/fallback/tickets
                - name: Retry
                  args:
                    retries: 3
                    statuses: BAD_GATEWAY,SERVICE_UNAVAILABLE
                    methods: GET

resilience4j:
  circuitbreaker:
    instances:
      eventServiceCB:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
      ticketServiceCB:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s

management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, gateway
  endpoint:
    health:
      show-details: always
```

---

## Running the Service

### Prerequisites

- Java 21
- Maven 3.9+
- `discovery-service` running at `localhost:8761`
- `config-server` running at `localhost:8088`

### Steps

1. **Clone the repository** and navigate to the service directory:

   ```bash
   git clone <repo-url>
   cd api-gateway
   ```

2. **Start the required services first** (in order):

   ```bash
   # 1. Discovery service
   cd discovery-service && ./mvnw spring-boot:run

   # 2. Config server
   cd config-server && ./mvnw spring-boot:run

   # 3. API Gateway
   cd api-gateway && ./mvnw spring-boot:run
   ```

3. Verify the gateway is running:

   ```bash
   curl http://localhost:8080/actuator/health
   ```

> The config-server import is declared as `optional:`, so the gateway will still start if the config-server is unavailable — but it will have no routes configured until restarted with the config-server available.

---

## Running Tests

```bash
./mvnw test
```

The test profile uses a dedicated `src/test/resources/application.yml` that disables the config-server import and the Eureka client, so the context loads without requiring any external services.

---

## Health & Monitoring

Spring Boot Actuator exposes the following endpoints:

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/health` | Service health status and circuit breaker state |
| `GET /actuator/info` | Application info |
| `GET /actuator/metrics` | JVM and application metrics |
| `GET /actuator/gateway/routes` | List of all configured routes |
