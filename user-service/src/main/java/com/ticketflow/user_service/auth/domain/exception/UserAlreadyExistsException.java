package com.ticketflow.user_service.auth.domain.exception;

/**
 * Exception thrown when attempting to register a user with an email
 * address that already belongs to an existing account.
 *
 * @author TicketFlow Team
 */
public class UserAlreadyExistsException extends RuntimeException {

    /**
     * Constructs a new {@code UserAlreadyExistsException} with a descriptive message.
     *
     * @param email the email address that is already registered
     */
    public UserAlreadyExistsException(String email) {
        super(String.format("User with email '%s' already exists", email));
    }
}
