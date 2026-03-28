package com.ticketflow.user_service.auth.domain.exception;

/**
 * Exception thrown when login fails due to an unrecognized email address
 * or an incorrect password.
 *
 * @author TicketFlow Team
 */
public class InvalidCredentialsException extends RuntimeException {

    /**
     * Constructs a new {@code InvalidCredentialsException} with a generic
     * message that deliberately avoids revealing which credential was invalid.
     */
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
