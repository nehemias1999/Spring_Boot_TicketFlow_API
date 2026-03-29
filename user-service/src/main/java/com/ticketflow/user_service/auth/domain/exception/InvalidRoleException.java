package com.ticketflow.user_service.auth.domain.exception;

/**
 * Exception thrown when an invalid role name is provided.
 *
 * @author TicketFlow Team
 */
public class InvalidRoleException extends RuntimeException {

    public InvalidRoleException(String role) {
        super(String.format("'%s' is not a valid role", role));
    }
}
