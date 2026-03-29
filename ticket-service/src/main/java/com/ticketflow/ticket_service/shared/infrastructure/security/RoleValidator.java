package com.ticketflow.ticket_service.shared.infrastructure.security;

import com.ticketflow.ticket_service.booking.domain.exception.AccessDeniedException;

/**
 * Stateless utility for role-based authorization checks.
 *
 * @author TicketFlow Team
 */
public final class RoleValidator {

    private RoleValidator() {
    }

    public static void requireAnyRole(String userRole, String... allowedRoles) {
        for (String allowed : allowedRoles) {
            if (allowed.equalsIgnoreCase(userRole)) {
                return;
            }
        }
        throw new AccessDeniedException(
                "Role '" + userRole + "' is not permitted for this operation");
    }

    public static boolean hasRole(String userRole, String role) {
        return role.equalsIgnoreCase(userRole);
    }
}
