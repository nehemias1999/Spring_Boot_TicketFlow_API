package com.ticketflow.notification_service.delivery.infrastructure.in.event;

/**
 * Local DTO for deserializing the {@code ticket.cancelled} RabbitMQ message.
 *
 * @param ticketId  the ID of the cancelled ticket
 * @param userId    the ID of the user who cancelled the ticket
 * @param userEmail the email address of the user
 */
public record TicketCancelledMessage(String ticketId, String userId, String userEmail) {
}
