package com.ticketflow.ticket_service.booking.domain.exception;

public class EventFullException extends RuntimeException {

    public EventFullException(String eventId) {
        super("Event '" + eventId + "' has no available tickets");
    }
}
