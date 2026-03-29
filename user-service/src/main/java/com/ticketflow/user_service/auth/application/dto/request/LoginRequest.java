package com.ticketflow.user_service.auth.application.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for the user login endpoint.
 * <p>
 * The {@code identifier} field accepts either an email address or a username.
 * The service layer detects the format by checking for the presence of {@code @}.
 * </p>
 *
 * @param identifier the user's email address or username
 * @param password   the user's plain-text password for verification
 * @author TicketFlow Team
 */
public record LoginRequest(

        @NotBlank(message = "Identifier (email or username) is required")
        String identifier,

        @NotBlank(message = "Password is required")
        String password
) {
}
