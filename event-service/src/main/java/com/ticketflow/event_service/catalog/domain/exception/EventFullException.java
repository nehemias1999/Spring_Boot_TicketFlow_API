package com.ticketflow.event_service.catalog.domain.exception;

public class EventFullException extends RuntimeException {

    public EventFullException(String eventId) {
        super("Event '" + eventId + "' has no available tickets");
    }
}
