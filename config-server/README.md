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

All sensitive values in the config files (JWT secret, database credentials, internal API key, RabbitMQ credentials, mail settings) reference environment variables. This way the config files can be committed to version control without exposing secrets.

The server also registers itself with the Eureka discovery service so that clients can locate it by name instead of a hardcoded URL.

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
│   ├── ticket-service.yml                                      │
│   ├── user-service.yml                                        │
│   └── notification-service.yml                                │
└────┬──────────┬──────────┬──────────┬───────────┬────────────┘
     │ fetches  │ fetches  │ fetches  │ fetches   │ fetches
     ▼          ▼          ▼          ▼           ▼
api-gateway event-service ticket-service user-service notification-service
(on startup) (on startup) (on startup)  (on startup) (on startup)
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

Each file under `src/main/resources/config/` maps to a microservice by its application name. Sensitive values are templated as `${ENV_VAR:default}`.

### `api-gateway.yml`

Key settings: JWT secret (`${JWT_SECRET}`), CORS allowed origins (`${CORS_ALLOWED_ORIGINS}`), route definitions for event-service and ticket-service, Resilience4j circuit breaker instances, retry config.

### `event-service.yml`

Key settings: server port `8081`, Eureka URL (`${EUREKA_URL}`), internal API key (`${INTERNAL_API_KEY}`), Jackson date format, actuator endpoints via `${MANAGEMENT_ENDPOINTS_INCLUDE}`.

### `ticket-service.yml`

Key settings: server port `8082`, Eureka URL, internal API key, RabbitMQ connection, actuator endpoints.

### `user-service.yml`

Key settings: server port `8084`, Eureka URL, JWT secret and expiration (`${JWT_SECRET}`, `${JWT_EXPIRATION_MS}`), RabbitMQ connection.

### `notification-service.yml`

Key settings: server port `8083`, Eureka URL, RabbitMQ connection, mail settings (`${MAIL_HOST}`, `${MAIL_PORT}`, `${MAIL_USERNAME}`, `${MAIL_PASSWORD}`, `${MAIL_SMTP_AUTH}`, `${MAIL_SMTP_STARTTLS}`, `${MAIL_FROM}`).

---

## Configuration API

The Config Server exposes standard Spring Cloud Config endpoints:

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/{application}/{profile}` | Fetch config for an application and profile |
| `GET` | `/{application}-{profile}.yml` | Raw YAML for an application+profile |

**Examples:**

```bash
# Fetch event-service configuration
GET http://localhost:8088/event-service/default

# Fetch notification-service configuration as raw YAML
GET http://localhost:8088/notification-service-default.yml
```

---

## Configuration

Key properties from `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: config-server
  profiles:
    active: native
  cloud:
    config:
      server:
        native:
          searchLocations: classpath:/config/

server:
  port: 8088

eureka:
  client:
    serviceUrl:
      defaultZone: ${EUREKA_URL:http://localhost:8761/eureka/}
  instance:
    prefer-ip-address: true
```

---

## Running the Service

### Prerequisites

- Java 21, Maven 3.9+
- `discovery-service` running at `localhost:8761`

```bash
cd config-server
./mvnw spring-boot:run
```

Verify by fetching any service config:

```bash
curl http://localhost:8088/event-service/default
```

---

## Running Tests

```bash
./mvnw test
```

The test profile keeps the `native` profile active with the classpath search location, and disables the Eureka client so the context loads without external services.

---

## Health & Monitoring

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/health` | Service health status |
| `GET /actuator/info` | Application info |
