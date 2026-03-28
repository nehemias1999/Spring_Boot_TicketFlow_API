package com.ticketflow.user_service.auth.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for the user login endpoint.
 * <p>
 * Contains the credentials required to authenticate an existing user
 * and receive a JWT token in return.
 * </p>
 *
 * @param email    the user's registered email address
 * @param password the user's plain-text password for verification
 * @author TicketFlow Team
 */
public record LoginRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {
}
