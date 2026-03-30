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

`notification-service` is a stateless, event-driven microservice. It does not expose any REST endpoints — all input arrives through RabbitMQ. Three types of events are handled:

| Event | Queue | Trigger | Email sent |
|-------|-------|---------|-----------|
| `ticket.purchased` | `ticket.purchased.queue` | Ticket created | "Your ticket has been confirmed" |
| `ticket.cancelled` | `ticket.cancelled.queue` | Ticket cancelled | "Your ticket has been cancelled" |
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
│   TicketCancelledEventListener  (@RabbitListener)         │
│   queue: ticket.cancelled.queue                           │
│                                                           │
│   UserRegisteredEventListener   (@RabbitListener)         │
│   queue: user.registered.queue                            │
└────────────────────────┬─────────────────────────────────┘
                         │ delegates to
┌────────────────────────▼─────────────────────────────────┐
│                   Application Layer                       │
│   ProcessNotificationUseCase    (ticket purchased)        │
│   ProcessCancellationNotificationUseCase (ticket cancelled)│
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
| `delivery.application.usecase` | Business logic — notification use cases |
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

Published by `ticket-service` when a ticket is successfully created (after DB commit).

```json
{
  "ticketId":  "3f2504e0-4f89-11d3-9a0c-0305e82c3301",
  "userId":    "usr_abc123",
  "userEmail": "user@example.com"
}
```

**Processing flow:**
1. `TicketPurchasedEventListener` receives from `ticket.purchased.queue`
2. `ProcessNotificationUseCase.execute(ticketId, userId, userEmail)` builds a `Notification`
3. Subject: `"TicketFlow — Your ticket has been confirmed"`
4. `JavaMailEmailSenderAdapter` sends via SMTP to `userEmail`

---

### `TicketCancelledMessage`

Published by `ticket-service` when a ticket is successfully cancelled (after DB commit).

```json
{
  "ticketId":  "3f2504e0-4f89-11d3-9a0c-0305e82c3301",
  "userId":    "usr_abc123",
  "userEmail": "user@example.com"
}
```

**Processing flow:**
1. `TicketCancelledEventListener` receives from `ticket.cancelled.queue`
2. `ProcessCancellationNotificationUseCase.execute(ticketId, userId, userEmail)` builds a `Notification`
3. Subject: `"TicketFlow — Your ticket has been cancelled"`
4. `JavaMailEmailSenderAdapter` sends via SMTP to `userEmail`

---

### `UserRegisteredMessage`

Published by `user-service` when a new user is successfully registered (after DB commit).

```json
{
  "userId": "usr_abc123",
  "email":  "user@example.com"
}
```

**Processing flow:**
1. `UserRegisteredEventListener` receives from `user.registered.queue`
2. `ProcessWelcomeNotificationUseCase.execute(userId, email)` builds a `Notification`
3. Subject: `"TicketFlow — Welcome to TicketFlow!"`
4. `JavaMailEmailSenderAdapter` sends via SMTP to `email`

---

All messages are serialized as JSON using `Jackson2JsonMessageConverter`.

---

## RabbitMQ Topology

| Element | Name | Type | Details |
|---------|------|------|---------|
| Exchange | `ticketflow.events` | Topic | Durable |
| Queue | `ticket.purchased.queue` | — | Durable, routing key `ticket.purchased` |
| Queue | `ticket.cancelled.queue` | — | Durable, routing key `ticket.cancelled` |
| Queue | `user.registered.queue` | — | Durable, routing key `user.registered` |

The exchange and queues are declared in `RabbitMQConfig` and created automatically on startup if they do not exist.

---

## Configuration

All sensitive values are injected via environment variables:

| Variable | Description |
|----------|-------------|
| `RABBITMQ_HOST` | RabbitMQ hostname (default `localhost`) |
| `RABBITMQ_PORT` | RabbitMQ AMQP port (default `5672`) |
| `RABBITMQ_USERNAME` | RabbitMQ username |
| `RABBITMQ_PASSWORD` | RabbitMQ password |
| `MAIL_HOST` | SMTP host (default `localhost` / MailHog) |
| `MAIL_PORT` | SMTP port (default `1025`) |
| `MAIL_USERNAME` | SMTP username (blank for MailHog) |
| `MAIL_PASSWORD` | SMTP password (blank for MailHog) |
| `MAIL_SMTP_AUTH` | Enable SMTP auth (`false` for MailHog) |
| `MAIL_SMTP_STARTTLS` | Enable STARTTLS (`false` for MailHog) |
| `MAIL_FROM` | From address (default `noreply@ticketflow.com`) |
| `EUREKA_URL` | Eureka server URL |

For production SMTP (e.g., SendGrid, SES), set `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH=true`, and `MAIL_SMTP_STARTTLS=true` in your `.env` file.

---

## Running the Service

### Option A — Docker Compose (recommended)

```bash
# From the repository root
cp .env.example .env   # fill in secrets on first run
docker-compose up -d
```

### Option B — Docker (standalone)

```bash
docker build -t ticketflow/notification-service ./notification-service
docker run -p 8083:8083 \
  -e RABBITMQ_HOST=host.docker.internal \
  -e RABBITMQ_USERNAME=ticketflow \
  -e RABBITMQ_PASSWORD=your-password \
  -e MAIL_HOST=host.docker.internal \
  -e MAIL_PORT=1025 \
  -e MAIL_SMTP_AUTH=false \
  -e MAIL_SMTP_STARTTLS=false \
  -e EUREKA_URL=http://host.docker.internal:8761/eureka/ \
  ticketflow/notification-service
```

### Option C — Maven (local development)

#### Prerequisites

- Java 21, Maven 3.9+
- RabbitMQ on port `5672`
- MailHog on port `1025` (or a real SMTP server)
- `discovery-service` and `config-server` running (optional — import is `optional:`)

```bash
cd notification-service
./mvnw spring-boot:run
```

Once started, the service registers with Eureka and begins listening on all three queues. No HTTP endpoints are exposed (Actuator health check only).

---

## Running Tests

```bash
./mvnw test
```

Tests run without a live RabbitMQ broker or SMTP server — dependencies are mocked. The test suite includes:

| Test class | Type | Description |
|------------|------|-------------|
| `ProcessNotificationUseCaseTest` | Unit | Builds correct `Notification` and calls `EmailSenderPort` |
| `TicketPurchasedEventListenerTest` | Unit | Listener delegates to use case with correct arguments |
| `NotificationServiceApplicationTests` | Integration | Spring context loads with mocked `ConnectionFactory` |
