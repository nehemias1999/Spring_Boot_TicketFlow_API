package com.ticketflow.ticket_service.booking.domain.exception;

/**
 * Thrown when an authenticated user attempts to modify a ticket they do not own.
 */
public class TicketOwnershipException extends RuntimeException {

    public TicketOwnershipException(String ticketId) {
        super("You do not have permission to modify ticket with id '" + ticketId + "'");
    }
}
