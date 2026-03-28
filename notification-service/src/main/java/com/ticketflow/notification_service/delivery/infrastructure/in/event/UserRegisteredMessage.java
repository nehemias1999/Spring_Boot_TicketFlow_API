package com.ticketflow.notification_service.delivery.infrastructure.in.event;

/**
 * Local DTO for deserializing the {@code user.registered} RabbitMQ message.
 *
 * @param userId the ID of the newly registered user
 * @param email  the email address of the newly registered user
 */
public record UserRegisteredMessage(String userId, String email) {
}
