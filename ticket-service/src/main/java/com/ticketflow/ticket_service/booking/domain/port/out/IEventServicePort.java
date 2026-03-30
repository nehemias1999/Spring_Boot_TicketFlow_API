package com.ticketflow.ticket_service.booking.domain.port.out;

import com.ticketflow.ticket_service.booking.application.dto.external.EventDto;

import java.util.List;

/**
 * Outbound port for querying and updating event data from the event-service.
 *
 * @author TicketFlow Team
 */
public interface IEventServicePort {

    /**
     * Returns the IDs of all active events created by the given seller.
     *
     * @param sellerId the user ID of the SELLER
     * @return list of event IDs; empty list if the call fails or the seller has no events
     */
    List<String> getSellerEventIds(String sellerId);

    /**
     * Fetches a single event by its ID. Throws EventNotFoundException if not found.
     *
     * @param eventId the event ID
     * @return the event data
     */
    EventDto getEventById(String eventId);

    /**
     * Decrements the available ticket count for the given event.
     * Throws EventFullException if no tickets are available.
     *
     * @param eventId the event ID
     */
    void decrementAvailableTickets(String eventId);

    /**
     * Increments the available ticket count for the given event (called on cancellation).
     *
     * @param eventId the event ID
     */
    void incrementAvailableTickets(String eventId);
}
