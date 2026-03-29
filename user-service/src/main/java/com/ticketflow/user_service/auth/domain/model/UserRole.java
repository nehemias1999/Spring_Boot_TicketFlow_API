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
     * This is the default role assigned to every new registration.
     */
    USER,

    /**
     * Moderator role. Can view all tickets and events (read-only)
     * and change the role of USER/SELLER users.
     */
    MODERATOR,

    /**
     * Seller role. Can create and manage their own events,
     * and view tickets for the events they created.
     */
    SELLER,

    /**
     * Administrator role with full access across the platform.
     */
    ADMIN
}
