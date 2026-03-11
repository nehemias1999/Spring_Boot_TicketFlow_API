package com.ticketflow.event_service.catalog.domain.exception;

/**
 * Exception thrown when attempting to create an event entry with an ID
 * that already exists in the system.
 *
 * @author TicketFlow Team
 */
public class EventAlreadyExistsException extends RuntimeException {

    /**
     * Constructs a new {@code EventAlreadyExistsException} with a descriptive message.
     *
     * @param id the duplicate event ID
     */
    public EventAlreadyExistsException(String id) {
        super(String.format("Event with id '%s' already exists", id));
    }
}

