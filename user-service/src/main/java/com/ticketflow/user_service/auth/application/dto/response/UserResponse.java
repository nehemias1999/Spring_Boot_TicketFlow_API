package com.ticketflow.user_service.auth.application.dto.response;

import java.time.LocalDateTime;

/**
 * Response DTO representing a user's non-sensitive profile data.
 *
 * @param id        the unique identifier of the user
 * @param username  the user's username
 * @param email     the user's email address
 * @param role      the role assigned to the user (e.g., "USER", "ADMIN")
 * @param createdAt the timestamp when the user account was created
 * @param updatedAt the timestamp when the user account was last updated
 * @author TicketFlow Team
 */
public record UserResponse(
        String id,
        String username,
        String email,
        String role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
