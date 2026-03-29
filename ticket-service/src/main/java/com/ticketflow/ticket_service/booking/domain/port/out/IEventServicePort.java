package com.ticketflow.ticket_service.booking.domain.port.out;

import java.util.List;

/**
 * Outbound port for querying event data from the event-service.
 * Used by the ticket-service to retrieve a SELLER's event IDs
 * when listing tickets for their events.
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
}
