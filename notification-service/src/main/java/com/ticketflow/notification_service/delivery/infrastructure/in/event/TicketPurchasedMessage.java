package com.ticketflow.notification_service.delivery.infrastructure.in.event;

/**
 * Local DTO for deserializing the {@code ticket.purchased} RabbitMQ message.
 *
 * @param ticketId  the ID of the purchased ticket
 * @param userId    the ID of the purchasing user
 * @param userEmail the email address of the purchasing user
 */
public record TicketPurchasedMessage(String ticketId, String userId, String userEmail) {
}
