# notification-service

![Java 21](https://img.shields.io/badge/Java-21-blue)
![Spring Boot 3.5.4](https://img.shields.io/badge/Spring%20Boot-3.5.4-brightgreen)
![Spring Cloud 2025.0.0](https://img.shields.io/badge/Spring%20Cloud-2025.0.0-brightgreen)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-AMQP-orange)

Microservice responsible for delivering **email notifications** in the TicketFlow platform. Listens to events from RabbitMQ and sends emails asynchronously — no REST endpoints are exposed.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Tech Stack](#tech-stack)
4. [Message Contracts](#message-contracts)
5. [RabbitMQ Topology](#rabbitmq-topology)
6. [Configuration](#configuration)
7. [Running the Service](#running-the-service)
8. [Running Tests](#running-tests)

---

## Overview

`notification-service` is a stateless, event-driven microservice. It does not expose any REST endpoints — all input arrives through RabbitMQ. Two types of events are handled:

| Event | Queue | Trigger | Email sent |
|-------|-------|---------|-----------|
| `ticket.purchased` | `ticket.purchased.queue` | Ticket created | "Your ticket has been confirmed" |
| `user.registered` | `user.registered.queue` | User registered | "Welcome to TicketFlow!" |

Email delivery is handled by `JavaMailEmailSenderAdapter` via JavaMailSender (SMTP). In development, MailHog is used as the SMTP server — inspect sent emails at `http://localhost:8025`.

The service registers with Eureka and fetches its configuration from the Config Server at startup.

---

## Architecture

The service follows **Hexagonal Architecture (Ports & Adapters)**, keeping notification use cases isolated from both the message broker and the email delivery mechanism.

```
┌──────────────────────────────────────────────────────────┐
│                    Inbound Adapters                       │
│   TicketPurchasedEventListener  (@RabbitListener)         │
│   queue: ticket.purchased.queue                           │
│                                                           │
│   UserRegisteredEventListener   (@RabbitListener)         │
│   queue: user.registered.queue                            │
└────────────────────────┬─────────────────────────────────┘
                         │ delegates to
┌────────────────────────▼─────────────────────────────────┐
│                   Application Layer                       │
│   ProcessNotificationUseCase    (ticket purchased)        │
│   ProcessWelcomeNotificationUseCase  (user registered)    │
│   build Notification → call EmailSenderPort               │
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
│   JavaMailEmailSenderAdapter  (sends via SMTP/MailHog)    │
└──────────────────────────────────────────────────────────┘
```

### Package overview

| Package | Responsibility |
|---------|---------------|
| `delivery.infrastructure.in.event` | Inbound AMQP adapters — listeners and message DTOs |
| `delivery.application.usecase` | Business logic — `ProcessNotificationUseCase`, `ProcessWelcomeNotificationUseCase` |
| `delivery.application.port.out` | Outbound port interface — `EmailSenderPort` |
| `delivery.domain` | Domain record — `Notification` |
| `delivery.infrastructure.out.email` | Outbound adapter — `JavaMailEmailSenderAdapter` |
| `delivery.infrastructure.config` | RabbitMQ infrastructure configuration |

---

## Tech Stack

| Category | Technology |
|----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.4 |
| Cloud | Spring Cloud 2025.0.0 (Config, Eureka) |
| Messaging | Spring AMQP, RabbitMQ |
| Email | JavaMailSender, MailHog (development SMTP) |
| Monitoring | Spring Boot Actuator |
| Testing | JUnit 5, Mockito, spring-rabbit-test |
| Build | Maven |
| Utils | Lombok |

---

## Message Contracts

### `TicketPurchasedMessage`

Published by `ticket-service` when a ticket is successfully created.

```json
{
  "ticketId":  "3f2504e0-4f89-11d3-9a0c-0305e82c3301",
  "userId":    "usr_abc123",
  "userEmail": "user@example.com"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `ticketId` | String | ID of the purchased ticket |
| `userId` | String | ID of the user who purchased the ticket |
| `userEmail` | String | Email address for the notification |

#### Processing flow

1. `TicketPurchasedEventListener` receives the message from `ticket.purchased.queue`
2. `ProcessNotificationUseCase.execute(ticketId, userId, userEmail)` is called
3. A `Notification` record is built with subject `"TicketFlow — Your ticket has been confirmed"`
4. `JavaMailEmailSenderAdapter` sends the email via SMTP to `userEmail`

---

### `UserRegisteredMessage`

Published by `user-service` when a new user is successfully registered.

```json
{
  "userId": "usr_abc123",
  "email":  "user@example.com"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `userId` | String | ID of the newly registered user |
| `email` | String | Email address for the welcome notification |

#### Processing flow

1. `UserRegisteredEventListener` receives the message from `user.registered.queue`
2. `ProcessWelcomeNotificationUseCase.execute(userId, email)` is called
3. A `Notification` record is built with subject `"TicketFlow — Welcome to TicketFlow!"`
4. `JavaMailEmailSenderAdapter` sends the welcome email via SMTP to `email`

---

Both messages are serialized as JSON using `Jackson2JsonMessageConverter`.

---

## RabbitMQ Topology

| Element | Name | Type | Details |
|---------|------|------|---------|
| Exchange | `ticketflow.events` | Topic | Durable |
| Queue | `ticket.purchased.queue` | — | Durable, routing key `ticket.purchased` |
| Queue | `user.registered.queue` | — | Durable, routing key `user.registered` |

The exchange and queues are declared in `RabbitMQConfig` and created automatically on startup if they do not exist.

---

## Configuration

Key properties from `src/main/resources/application.yml`:

```yaml
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
spring:
  mail:
    host: localhost
    port: 1025
    properties:
      mail:
        smtp:
          auth: false
          starttls:
            enable: false

notification:
  mail:
    from: noreply@ticketflow.com

eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
```

> MailHog must be running before starting this service. Start it with `docker-compose -f infra/docker-compose.yml up -d`. Inspect sent emails at `http://localhost:8025`.

---

## Running the Service

### Prerequisites

- Java 21
- Maven 3.9+
- RabbitMQ running locally on port `5672`
- MailHog running locally on port `1025` (SMTP)
- `discovery-service` and `config-server` running (optional — import is `optional:`)

### Steps

```bash
cd notification-service
./mvnw spring-boot:run
```

Once started, the service registers with Eureka and begins listening on both `ticket.purchased.queue` and `user.registered.queue`. No HTTP endpoints are exposed.

---

## Running Tests

```bash
./mvnw test
```

Tests run without a live RabbitMQ broker or SMTP server — dependencies are mocked in the test context.

The test suite includes:

| Test class | Type | Description |
|------------|------|-------------|
| `ProcessNotificationUseCaseTest` | Unit | Verifies the use case builds the correct `Notification` and calls `EmailSenderPort` |
| `TicketPurchasedEventListenerTest` | Unit | Verifies the listener delegates to the use case with the correct arguments |
| `NotificationServiceApplicationTests` | Integration | Verifies the Spring context loads with a mocked `ConnectionFactory` |
