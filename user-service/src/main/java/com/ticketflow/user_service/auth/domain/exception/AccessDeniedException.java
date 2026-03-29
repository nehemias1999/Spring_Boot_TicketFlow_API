package com.ticketflow.user_service.auth.domain.exception;

/**
 * Exception thrown when an authenticated user attempts an operation
 * they are not authorized to perform.
 *
 * @author TicketFlow Team
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
