package com.ticketflow.user_service.auth.application.dto.response;

/**
 * Response DTO returned by the authentication endpoints (register and login).
 *
 * @param token    the signed JWT token to be included in subsequent requests
 * @param userId   the unique identifier of the authenticated user
 * @param username the username of the authenticated user
 * @param email    the email address of the authenticated user
 * @param role     the role assigned to the authenticated user (e.g., "USER", "ADMIN")
 * @author TicketFlow Team
 */
public record AuthResponse(
        String token,
        String userId,
        String username,
        String email,
        String role
) {
}
