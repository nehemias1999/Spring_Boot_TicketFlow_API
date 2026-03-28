package com.ticketflow.user_service.auth.domain.port.out;

/**
 * Outbound port for publishing user domain events.
 * <p>
 * Pure Java interface — no Spring or AMQP dependencies.
 * </p>
 */
public interface IUserEventPublisher {

    void publishUserRegistered(String userId, String email);
}
