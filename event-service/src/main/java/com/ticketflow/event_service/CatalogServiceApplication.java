package com.ticketflow.event_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Main entry point for the Event Catalog microservice.
 * <p>
 * This Spring Boot application bootstraps the service, enabling
 * auto-configuration and component scanning across all sub-packages
 * under {@code com.ticketflow.event_service}.
 * </p>
 * <p>
 * The {@link EnableDiscoveryClient} annotation registers this service
 * with the Eureka Service Discovery server, allowing the API Gateway
 * and other microservices to discover and communicate with it dynamically.
 * </p>
 *
 * @author TicketFlow Team
 */
@SpringBootApplication
@EnableDiscoveryClient
public class CatalogServiceApplication {

	/**
	 * Starts the Spring Boot application.
	 *
	 * @param args command-line arguments passed at startup
	 */
	public static void main(String[] args) {
		SpringApplication.run(CatalogServiceApplication.class, args);
	}

}
