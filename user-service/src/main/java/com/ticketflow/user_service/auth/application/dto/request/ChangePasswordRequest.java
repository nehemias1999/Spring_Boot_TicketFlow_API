package com.ticketflow.user_service.auth.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for changing a user's password.
 *
 * @param currentPassword the user's current password for verification
 * @param newPassword      the new password (minimum 8 characters)
 * @author TicketFlow Team
 */
public record ChangePasswordRequest(

        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "New password must be at least 8 characters")
        String newPassword
) {
}
