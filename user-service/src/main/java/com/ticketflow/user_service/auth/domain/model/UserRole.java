package com.ticketflow.user_service.auth.domain.model;

/**
 * Enumeration of roles that can be assigned to a user.
 * <p>
 * Used for authorization decisions both within the user-service
 * and by downstream services that decode the JWT claims.
 * </p>
 *
 * @author TicketFlow Team
 */
public enum UserRole {

    /**
     * Standard user role with access to ticket purchasing and management.
     */
    USER,

    /**
     * Administrator role with elevated access across the platform.
     */
    ADMIN
}
