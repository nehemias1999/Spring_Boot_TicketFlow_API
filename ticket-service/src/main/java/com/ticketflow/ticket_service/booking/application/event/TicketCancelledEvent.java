package com.ticketflow.ticket_service.booking.application.event;

/**
 * Event DTO published when a ticket is successfully cancelled.
 * <p>
 * Pure Java record — no Spring or AMQP dependencies.
 * </p>
 *
 * @param ticketId  the ID of the cancelled ticket
 * @param userId    the ID of the user who cancelled the ticket
 * @param userEmail the email address of the user (used by notification-service)
 */
public record TicketCancelledEvent(String ticketId, String userId, String userEmail) {
}
