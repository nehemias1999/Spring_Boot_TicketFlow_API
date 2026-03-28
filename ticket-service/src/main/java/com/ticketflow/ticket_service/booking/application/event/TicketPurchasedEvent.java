package com.ticketflow.ticket_service.booking.application.event;

/**
 * Event DTO published when a ticket is successfully purchased.
 * <p>
 * Pure Java record — no Spring or AMQP dependencies.
 * </p>
 *
 * @param ticketId  the ID of the purchased ticket
 * @param userId    the ID of the user who purchased the ticket
 * @param userEmail the email address of the user (used by notification-service)
 */
public record TicketPurchasedEvent(String ticketId, String userId, String userEmail) {
}
