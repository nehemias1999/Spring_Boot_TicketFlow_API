package com.ticketflow.user_service.auth.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for the user registration endpoint.
 * <p>
 * Contains the minimum required fields to create a new user account.
 * The password is accepted as plain text and hashed by the service layer.
 * </p>
 *
 * @param email    the user's email address; must be a valid email format and at most 255 characters
 * @param password the user's plain-text password; must be between 6 and 100 characters
 * @author TicketFlow Team
 */
public record RegisterRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
        String password
) {
}
