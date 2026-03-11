package com.ticketflow.event_service.catalog.domain.exception;

/**
 * Exception thrown when an event entry with the specified ID is not found
 * or has been soft-deleted.
 *
 * @author TicketFlow Team
 */
public class EventNotFoundException extends RuntimeException {

    /**
     * Constructs a new {@code EventNotFoundException} with a descriptive message.
     *
     * @param id the event ID that was not found
     */
    public EventNotFoundException(String id) {
        super(String.format("Event with id '%s' not found", id));
    }
}

