# notification-service

![Java 21](https://img.shields.io/badge/Java-21-blue)
![Spring Boot 3.5.4](https://img.shields.io/badge/Spring%20Boot-3.5.4-brightgreen)
![Spring Cloud 2025.0.0](https://img.shields.io/badge/Spring%20Cloud-2025.0.0-brightgreen)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-AMQP-orange)

Microservice responsible for delivering email notifications in the **TicketFlow** platform. Listens to ticket purchase events from RabbitMQ and notifies the corresponding user asynchronously.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Tech Stack](#tech-stack)
4. [Message Contract](#message-contract)
5. [RabbitMQ Topology](#rabbitmq-topology)
6. [Configuration](#configuration)
7. [Running the Service](#running-the-service)
8. [Running Tests](#running-tests)

---

## Overview

`notification-service` is a stateless, event-driven microservice. It does not expose any REST endpoints — all input arrives through RabbitMQ. When a ticket is purchased, `ticket-service` publishes a `TicketPurchasedMessage` to the `ticketflow.events` exchange. This service picks it up from the `ticket.purchased.queue`, builds a `Notification`, and delegates delivery to the `EmailSenderPort`.

The current `EmailSenderPort` implementation (`ConsoleEmailSenderAdapter`) logs the notification to the console as a placeholder. It is designed to be replaced by an SMTP or third-party email adapter without touching the domain or use-case logic.

The service registers with Eureka and fetches its configuration from the Config Server at startup.

---

## Architecture

The service follows **Hexagonal Architecture (Ports & Adapters)**, keeping the notification use case isolated from both the message broker and the email delivery mechanism.

```
┌──────────────────────────────────────────────────────────┐
│                    Inbound Adapter                        │
│   TicketPurchasedEventListener  (@RabbitListener)         │
│   queue: ticket.purchased.queue                           │
└────────────────────────┬─────────────────────────────────┘
                         │ delegates to
┌────────────────────────▼─────────────────────────────────┐
│                   Application Layer                       │
│   ProcessNotificationUseCase                              │
│   builds Notification → calls EmailSenderPort             │
└────────────────────────┬─────────────────────────────────┘
                         │ uses port out
┌────────────────────────▼─────────────────────────────────┐
│                     Domain Layer                          │
│   Notification (record)                                   │
│   EmailSenderPort (port out interface)                    │
└────────────────────────┬─────────────────────────────────┘
                         │ implements port out
┌────────────────────────▼─────────────────────────────────┐
│                   Outbound Adapter                        │
│   ConsoleEmailSenderAdapter  (logs to console)            │
│   → replace with SMTP/SES adapter for production          │
└──────────────────────────────────────────────────────────┘
```

### Package overview

| Package | Responsibility |
|---------|---------------|
| `delivery.infrastructure.in.event` | Inbound AMQP adapter — `TicketPurchasedEventListener`, `TicketPurchasedMessage` DTO |
| `delivery.application.usecase` | Business logic — `ProcessNotificationUseCase` |
| `delivery.application.port.out` | Outbound port interface — `EmailSenderPort` |
| `delivery.domain` | Domain record — `Notification` |
| `delivery.infrastructure.out.email` | Outbound adapter — `ConsoleEmailSenderAdapter` |
| `delivery.infrastructure.config` | RabbitMQ infrastructure configuration |

---

## Tech Stack

| Category | Technology |
|----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.4 |
| Cloud | Spring Cloud 2025.0.0 (Config, Eureka) |
| Messaging | Spring AMQP, RabbitMQ |
| Monitoring | Spring Boot Actuator |
| Testing | JUnit 5, Mockito, spring-rabbit-test |
| Build | Maven |
| Utils | Lombok |

---

## Message Contract

### `TicketPurchasedMessage`

Published by `ticket-service` when a ticket is successfully created.

```json
{
  "ticketId": "3f2504e0-4f89-11d3-9a0c-0305e82c3301",
  "userId":   "user-001"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `ticketId` | String | ID of the purchased ticket |
| `userId` | String | ID of the user who purchased the ticket |

The message is serialized as JSON using `Jackson2JsonMessageConverter`.

### Processing flow

1. `TicketPurchasedEventListener` receives the message from `ticket.purchased.queue`
2. `ProcessNotificationUseCase.execute(ticketId, userId)` is called
3. A `Notification` record is created with a UUID, the message `"Your ticket {ticketId} has been confirmed"`, and status `"SENT"`
4. `EmailSenderPort.sendEmail(notification)` is invoked
5. `ConsoleEmailSenderAdapter` logs: `[EMAIL] To: {userId} | Message: {message}`

---

## RabbitMQ Topology

| Element | Name | Type | Details |
|---------|------|------|---------|
| Exchange | `ticketflow.events` | Topic | Durable |
| Queue | `ticket.purchased.queue` | — | Durable |
| Routing Key | `ticket.purchased` | — | Binds queue to exchange |

The exchange and queue are declared in `RabbitMQConfig` and created automatically on startup if they do not exist.

---

## Configuration

Key properties from `src/main/resources/application.yml`:

```yaml
server:
  port: 8083

spring:
  application:
    name: notification-service

  config:
    import: "optional:configserver:http://localhost:8088"

  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

Additional properties served by the Config Server (`notification-service.yml`):

```yaml
eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
```

> RabbitMQ must be running before starting this service. Default credentials are `guest`/`guest` on `localhost:5672`.

---

## Running the Service

### Prerequisites

- Java 21
- Maven 3.9+
- RabbitMQ running locally on port `5672`
- `discovery-service` and `config-server` running (optional — import is `optional:`)

### Steps

```bash
cd notification-service
./mvnw spring-boot:run
```

Once started, the service registers with Eureka and begins listening on `ticket.purchased.queue`. No HTTP endpoints are exposed.

---

## Running Tests

```bash
./mvnw test
```

Tests run without a live RabbitMQ broker — the `ConnectionFactory` is mocked in the integration test context.

The test suite includes:

| Test class | Type | Description |
|------------|------|-------------|
| `ProcessNotificationUseCaseTest` | Unit | Verifies the use case builds the correct `Notification` and calls `EmailSenderPort` |
| `TicketPurchasedEventListenerTest` | Unit | Verifies the listener delegates to the use case with the correct arguments |
| `ConsoleEmailSenderAdapterTest` | Unit | Verifies the adapter logs without throwing exceptions |
| `NotificationServiceApplicationTests` | Integration | Verifies the Spring context loads with a mocked `ConnectionFactory` |
