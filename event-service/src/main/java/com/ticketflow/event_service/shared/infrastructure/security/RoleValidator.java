package com.ticketflow.event_service.shared.infrastructure.security;

import com.ticketflow.event_service.catalog.domain.exception.AccessDeniedException;

/**
 * Stateless utility for role-based authorization checks.
 * <p>
 * Reads the role from the {@code X-User-Role} header propagated by the API Gateway.
 * No Spring Security dependency is required.
 * </p>
 *
 * @author TicketFlow Team
 */
public final class RoleValidator {

    private RoleValidator() {
    }

    /**
     * Throws {@link AccessDeniedException} if {@code userRole} is not in {@code allowedRoles}.
     *
     * @param userRole     the role from the request header
     * @param allowedRoles one or more roles that are permitted
     */
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
