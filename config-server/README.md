# config-server

![Java 21](https://img.shields.io/badge/Java-21-blue)
![Spring Boot 3.5.4](https://img.shields.io/badge/Spring%20Boot-3.5.4-brightgreen)
![Spring Cloud 2025.0.0](https://img.shields.io/badge/Spring%20Cloud-2025.0.0-brightgreen)

Centralized configuration server for the **TicketFlow** ticket reservation system. Serves externalized configuration files to all microservices at startup, enabling environment-specific settings without redeployment.

---

## Table of Contents

1. [Overview](#overview)
2. [Role in the Architecture](#role-in-the-architecture)
3. [Tech Stack](#tech-stack)
4. [Managed Configurations](#managed-configurations)
5. [Configuration API](#configuration-api)
6. [Configuration](#configuration)
7. [Running the Service](#running-the-service)
8. [Running Tests](#running-tests)
9. [Health & Monitoring](#health--monitoring)

---

## Overview

`config-server` is a **Spring Cloud Config Server** running in `native` profile mode, which means it reads configuration files directly from the classpath (`src/main/resources/config/`) rather than from a Git repository. Each microservice fetches its own configuration from this server on startup using its `spring.application.name` as the lookup key.

The server also registers itself with the Eureka discovery service so that clients can locate it by name (`config-server`) instead of a hardcoded URL.

---

## Role in the Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                       config-server                           │
│               (Spring Cloud Config Server)                    │
│                      localhost:8088                           │
│                                                               │
│   src/main/resources/config/                                  │
│   ├── api-gateway.yml                                         │
│   ├── event-service.yml                                       │
│   └── ticket-service.yml                                      │
└────────┬──────────────┬──────────────┬────────────────────────┘
         │ fetches      │ fetches      │ fetches
         ▼              ▼              ▼
    api-gateway    event-service  ticket-service
    (on startup)   (on startup)   (on startup)
```

**Startup order** — the config-server should start after the discovery-service and before any business microservice, since those services fetch their configuration on boot.

| Property | Value |
|----------|-------|
| Profile | `native` — reads files from classpath |
| Search location | `classpath:/config/` |
| Port | `8088` |

---

## Tech Stack

| Category | Technology |
|----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.4 |
| Cloud | Spring Cloud 2025.0.0 (Config Server, Eureka Client) |
| Monitoring | Spring Boot Actuator |
| Testing | JUnit 5 |
| Build | Maven |

---

## Managed Configurations

Each file under `src/main/resources/config/` maps to a microservice by its application name.

### `event-service.yml`

| Property | Value | Description |
|----------|-------|-------------|
| `server.port` | `8081` | HTTP port for the event-service |
| `eureka.client.serviceUrl.defaultZone` | `http://localhost:8761/eureka/` | Eureka registration URL |
| `eureka.instance.prefer-ip-address` | `true` | Register by IP instead of hostname |
| `management.endpoints.web.exposure.include` | `health, info, metrics, refresh` | Exposed Actuator endpoints |

### `ticket-service.yml`

| Property | Value | Description |
|----------|-------|-------------|
| `server.port` | `8082` | HTTP port for the ticket-service |
| `eureka.client.serviceUrl.defaultZone` | `http://localhost:8761/eureka/` | Eureka registration URL |
| `eureka.instance.prefer-ip-address` | `true` | Register by IP instead of hostname |
| `management.endpoints.web.exposure.include` | `health, info, metrics, refresh` | Exposed Actuator endpoints |

### `api-gateway.yml`

| Property | Value | Description |
|----------|-------|-------------|
| `server.port` | `8080` | HTTP port for the api-gateway |
| `spring.cloud.gateway.server.webflux.routes` | see below | Route definitions |
| `spring.cloud.gateway.globalcors` | `*` origins, all methods | Global CORS policy |
| `resilience4j.circuitbreaker.instances.eventServiceCB` | 50% threshold, 10s open | Circuit breaker for event-service |
| `resilience4j.circuitbreaker.instances.ticketServiceCB` | 50% threshold, 10s open | Circuit breaker for ticket-service |

**Defined routes:**

| Route ID | URI | Predicate | Filters |
|----------|-----|-----------|---------|
| `event-service` | `lb://event-service` | `Path=/api/v1/events/**` | CircuitBreaker + Retry (3 attempts, GET only) |
| `ticket-service` | `lb://ticket-service` | `Path=/api/v1/tickets/**` | CircuitBreaker + Retry (3 attempts, GET only) |

---

## Configuration API

The Config Server exposes standard Spring Cloud Config endpoints that clients use to fetch their properties:

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/{application}/{profile}` | Fetch config for an application and profile |
| `GET` | `/{application}/{profile}/{label}` | Fetch config for a specific label (branch/tag) |
| `GET` | `/{application}-{profile}.yml` | Raw YAML for an application+profile |

**Examples:**

```bash
# Fetch event-service configuration (default profile)
GET http://localhost:8088/event-service/default

# Fetch ticket-service configuration as raw YAML
GET http://localhost:8088/ticket-service-default.yml

# Fetch api-gateway configuration as raw YAML
GET http://localhost:8088/api-gateway-default.yml
```

---

## Configuration

Key properties from `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: config-server
  profiles:
    active: native              # read config files from classpath, not Git
  cloud:
    config:
      server:
        native:
          searchLocations: classpath:/config/   # explicit location of config files

server:
  port: 8088

eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true

management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics
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

### Steps

1. **Clone the repository** and navigate to the service directory:

   ```bash
   git clone <repo-url>
   cd config-server
   ```

2. **Start the discovery-service first**, then run the config-server:

   ```bash
   ./mvnw spring-boot:run
   ```

3. Verify the server is serving configuration:

   ```bash
   curl http://localhost:8088/event-service/default
   ```

> The config-server registers itself with Eureka so that microservices can locate it by name. If Eureka is not available, the server still starts and serves configuration — Eureka registration will be retried in the background.

---

## Running Tests

```bash
./mvnw test
```

The test profile uses a dedicated `src/test/resources/application.yml` that keeps the `native` profile active with the classpath search location, and disables the Eureka client so the context loads without requiring external services.

---

## Health & Monitoring

Spring Boot Actuator exposes the following endpoints:

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/health` | Service health status and details |
| `GET /actuator/info` | Application info |
| `GET /actuator/metrics` | JVM and application metrics |
