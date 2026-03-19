package com.ticketflow.ticket_service.booking.domain.model;

/**
 * Represents the lifecycle state of a ticket booking.
 *
 * @author TicketFlow Team
 */
public enum TicketStatus {

    /** The ticket has been successfully purchased and is active. */
    CONFIRMED,

    /** The ticket has been cancelled and is no longer valid. */
    CANCELLED
}
