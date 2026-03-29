package com.ticketflow.user_service.auth.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for the user registration endpoint.
 *
 * @param username the user's unique username; must be between 3 and 50 characters
 * @param email    the user's email address; must be a valid email format and at most 255 characters
 * @param password the user's plain-text password; must be between 6 and 100 characters
 * @author TicketFlow Team
 */
public record RegisterRequest(

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
        String password
) {
}
