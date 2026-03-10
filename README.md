API Gateway - TicketFlow Microservices Project
Overview
This project is part of the Spring Boot TicketFlow API, an incremental microservices system for ticket booking. The API Gateway acts as the single entry point for all client requests, routing them to the appropriate backend services and handling cross-cutting concerns such as security, logging, and resilience.

Technologies Used
Java 21
Spring Boot 3.x / 4.x
Spring Cloud Gateway
Spring Cloud Netflix Eureka (Service Discovery)
Spring Cloud Config (Centralized Configuration)
Maven
Netty (Reactive HTTP Server)
Docker (optional, for containerization)
Architecture
The project follows Hexagonal Architecture (Ports and Adapters) and Vertical Slicing principles. It is composed of several microservices:

API Gateway: Entry point for all external requests. Handles routing, load balancing, security, and resilience.
Config Server: Centralized configuration management for all services.
Discovery Service (Eureka Server): Service registry for dynamic discovery of microservices.
Event Service: Manages event catalog and ticketing logic.
All services communicate over HTTP using REST APIs. Service discovery and configuration are managed centrally.

Service Responsibilities
API Gateway:
Routes incoming requests to backend services.
Provides a single endpoint for clients.
Handles authentication, authorization, and request filtering.
Implements resilience patterns (circuit breaker, retries).
Config Server:
Stores and serves configuration properties for all microservices.
Enables dynamic configuration updates without redeploying services.
Discovery Service:
Registers all running microservices.
Enables dynamic service lookup and load balancing.
Event Service:
Manages events, tickets, and related business logic.
Exposes REST endpoints for event catalog operations.
How to Run the Project
Clone the repository:
git clone <repository-url>
cd Spring_Boot_TicketFlow_API
Start the Config Server:
cd config-server
./mvnw spring-boot:run
Start the Discovery Service:
cd discovery-service
./mvnw spring-boot:run
Start the Event Service:
cd event-service
./mvnw spring-boot:run
Start the API Gateway:
cd api-gateway
./mvnw spring-boot:run
Note: Ensure that ports used by each service (default: 8888 for config, 8761 for discovery, 8080/8081/8082 for services) are available.

Project Structure
Spring_Boot_TicketFlow_API/
├── api-gateway/         # API Gateway microservice
├── config-server/       # Centralized configuration server
├── discovery-service/   # Eureka service registry
├── event-service/       # Event and ticket management service
└── README.md            # Project overview
Purpose
The main goal of this project is to demonstrate:

Microservices communication patterns
Service discovery and registration
Centralized configuration
API Gateway routing and resilience
Clean, maintainable architecture using Hexagonal principles
Authors
[Your Name]
License
This project is licensed under the MIT License.