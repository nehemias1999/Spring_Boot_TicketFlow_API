package com.ticketflow.ticket_service.booking.domain.port.out;

/**
 * Outbound port for publishing ticket domain events.
 * <p>
 * Pure Java interface — no Spring or AMQP dependencies.
 * </p>
 */
public interface ITicketEventPublisher {

    void publishTicketPurchased(String ticketId, String userId, String userEmail);

    void publishTicketCancelled(String ticketId, String userId, String userEmail);
}
