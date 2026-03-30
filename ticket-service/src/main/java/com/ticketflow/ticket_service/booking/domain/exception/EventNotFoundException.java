package com.ticketflow.ticket_service.booking.domain.exception;

public class EventNotFoundException extends RuntimeException {

    public EventNotFoundException(String eventId) {
        super("Event '" + eventId + "' not found or is no longer available");
    }
}
