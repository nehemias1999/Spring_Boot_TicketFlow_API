# discovery-service

![Java 21](https://img.shields.io/badge/Java-21-blue)
![Spring Boot 3.5.4](https://img.shields.io/badge/Spring%20Boot-3.5.4-brightgreen)
![Spring Cloud 2025.0.0](https://img.shields.io/badge/Spring%20Cloud-2025.0.0-brightgreen)

Service registry for the **TicketFlow** ticket reservation system. Acts as the central Eureka Server where all microservices register themselves and discover each other by name instead of hardcoded URLs.

---

## Table of Contents

1. [Overview](#overview)
2. [Role in the Architecture](#role-in-the-architecture)
3. [Tech Stack](#tech-stack)
4. [Eureka Dashboard](#eureka-dashboard)
5. [Configuration](#configuration)
6. [Running the Service](#running-the-service)
7. [Running Tests](#running-tests)
8. [Health & Monitoring](#health--monitoring)

---

## Overview

`discovery-service` is a standalone **Netflix Eureka Server**. It does not register itself in the registry and does not fetch the registry from any peer — it is the source of truth for service locations. Every other microservice in TicketFlow registers with this server on startup and uses it to resolve the addresses of other services at runtime.

---

## Role in the Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                       discovery-service                           │
│                   (Netflix Eureka Server)                         │
│                        localhost:8761                             │
└───┬──────────┬──────────┬──────────┬──────────┬──────────────────┘
    │          │          │          │          │
    ▼          ▼          ▼          ▼          ▼
config-server api-gateway event-service ticket-service user-service
(port 8088)  (port 8080) (port 8081)  (port 8082)  (port 8084)
                                                 + notification-service
                                                   (port 8083)
```

**Startup order** — the discovery-service must be the first service started, as all other services attempt to register with Eureka on boot.

| Property | Value |
|----------|-------|
| `register-with-eureka` | `false` — the server does not register itself |
| `fetch-registry` | `false` — no peer replication in single-node setup |

---

## Tech Stack

| Category | Technology |
|----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.4 |
| Cloud | Spring Cloud 2025.0.0 (Netflix Eureka Server) |
| Monitoring | Spring Boot Actuator |
| Testing | JUnit 5 |
| Build | Maven |

---

## Eureka Dashboard

Once the service is running, the Eureka web dashboard is available at:

```
http://localhost:8761
```

The dashboard shows:
- All registered service instances with their status (`UP` / `DOWN`)
- Instance metadata (IP, port, health-check URL)
- General server information (environment, uptime, renewal threshold)

When all TicketFlow services are running you should see these instances registered:

- `API-GATEWAY`
- `CONFIG-SERVER`
- `EVENT-SERVICE`
- `TICKET-SERVICE`
- `USER-SERVICE`
- `NOTIFICATION-SERVICE`

---

## Configuration

Key properties from `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: discovery-service

server:
  port: 8761

eureka:
  instance:
    hostname: localhost
  client:
    register-with-eureka: false   # this node is the registry, not a client
    fetch-registry: false         # no peer replication in single-node setup
    serviceUrl:
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/

management:
  endpoints:
    web:
      exposure:
        include: health, info
  endpoint:
    health:
      show-details: when-authorized
```

---

## Running the Service

### Option A — Docker Compose (recommended)

The easiest way to run the full TicketFlow stack including this service:

```bash
# From the repository root
cp .env.example .env   # fill in secrets on first run
docker-compose up -d
```

Docker Compose builds the image automatically and starts all services in the correct dependency order.

### Option B — Docker (standalone)

```bash
# Build the image
docker build -t ticketflow/discovery-service ./discovery-service

# Run the container
docker run -p 8761:8761 ticketflow/discovery-service
```

### Option C — Maven (local development)

#### Prerequisites

- Java 21
- Maven 3.9+

```bash
cd discovery-service
./mvnw spring-boot:run
```

Verify the dashboard is available at `http://localhost:8761`.

> **Start this service first.** All other TicketFlow microservices register with Eureka on startup. Starting them before the discovery-service will cause registration errors (they will retry, but it is cleaner to start in order).

---

## Running Tests

```bash
./mvnw test
```

The test profile uses a dedicated `src/test/resources/application.yml` that assigns a random port (`0`) and disables peer replication, so the context loads without requiring a real Eureka instance.

---

## Health & Monitoring

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/health` | Service health status |
| `GET /actuator/info` | Application info |
