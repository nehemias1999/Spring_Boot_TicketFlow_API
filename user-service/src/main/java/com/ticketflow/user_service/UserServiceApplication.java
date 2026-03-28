package com.ticketflow.user_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the TicketFlow User Service.
 * <p>
 * Handles user registration, authentication, and JWT token issuance
 * for the TicketFlow microservices platform.
 * </p>
 *
 * @author TicketFlow Team
 */
@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
