package com.ticketflow.user_service.auth.domain.exception;

/**
 * Exception thrown when a user is not found by the given identifier.
 *
 * @author TicketFlow Team
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String id) {
        super(String.format("User with id '%s' not found", id));
    }
}
