package com.ticketflow.user_service.auth.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Core domain model representing a registered user.
 * <p>
 * This is a pure domain object with no infrastructure or framework dependencies.
 * It holds all business-relevant attributes for an authenticated user in the
 * TicketFlow platform.
 * </p>
 *
 * @author TicketFlow Team
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /**
     * Unique identifier for the user, generated as a UUID on registration.
     */
    private String id;

    /**
     * The user's unique username, used as an alternative login credential.
     */
    private String username;

    /**
     * The user's email address, used as the unique login credential.
     */
    private String email;

    /**
     * The user's BCrypt-hashed password. Never stored or transmitted as plain text.
     */
    private String password;

    /**
     * The role assigned to the user, determining their level of access.
     */
    private UserRole role;

    /**
     * Soft-delete flag. When {@code true}, the user is considered deleted
     * and is excluded from active queries.
     */
    @Builder.Default
    private boolean deleted = false;

    /**
     * Timestamp indicating when this user record was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp indicating when this user record was last updated.
     */
    private LocalDateTime updatedAt;
}
