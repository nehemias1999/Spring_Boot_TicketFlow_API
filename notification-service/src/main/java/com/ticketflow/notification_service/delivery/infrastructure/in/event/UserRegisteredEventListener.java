package com.ticketflow.notification_service.delivery.infrastructure.in.event;

import com.ticketflow.notification_service.delivery.application.usecase.ProcessWelcomeNotificationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Inbound AMQP adapter — listens on {@code user.registered.queue} and
 * delegates to {@link ProcessWelcomeNotificationUseCase}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredEventListener {

    private final ProcessWelcomeNotificationUseCase processWelcomeNotificationUseCase;

    @RabbitListener(queues = "user.registered.queue")
    public void onUserRegistered(UserRegisteredMessage message) {
        log.info("Received UserRegisteredMessage — userId: {}", message.userId());
        processWelcomeNotificationUseCase.execute(message.userId(), message.email());
    }
}
