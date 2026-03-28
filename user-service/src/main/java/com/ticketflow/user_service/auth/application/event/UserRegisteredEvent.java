package com.ticketflow.user_service.auth.application.event;

/**
 * Event DTO published when a new user registers successfully.
 * <p>
 * Pure Java record — no Spring or AMQP dependencies.
 * </p>
 *
 * @param userId the ID of the newly registered user
 * @param email  the email address of the newly registered user
 */
public record UserRegisteredEvent(String userId, String email) {
}
