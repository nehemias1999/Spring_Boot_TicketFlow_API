package com.ticketflow.user_service.auth.application.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for the update-role endpoint.
 *
 * @param role the target role name (e.g., "USER", "SELLER", "MODERATOR", "ADMIN")
 * @author TicketFlow Team
 */
public record UpdateRoleRequest(

        @NotBlank(message = "Role is required")
        String role
) {
}
