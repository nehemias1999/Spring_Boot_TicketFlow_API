package com.ticketflow.user_service.auth.infrastructure.adapter.out.messaging;

import com.ticketflow.user_service.auth.application.event.UserRegisteredEvent;
import com.ticketflow.user_service.auth.domain.port.out.IUserEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * AMQP adapter implementing {@link IUserEventPublisher}.
 * <p>
 * Publishes {@link UserRegisteredEvent} to the {@code ticketflow.events} topic exchange
 * with routing key {@code user.registered}.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMQUserEventPublisher implements IUserEventPublisher {

    static final String EXCHANGE = "ticketflow.events";
    static final String ROUTING_KEY = "user.registered";

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishUserRegistered(String userId, String email) {
        UserRegisteredEvent event = new UserRegisteredEvent(userId, email);
        log.info("Publishing UserRegisteredEvent — userId: {}", userId);
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event);
    }
}
