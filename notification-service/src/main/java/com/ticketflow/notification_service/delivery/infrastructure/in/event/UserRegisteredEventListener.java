package com.ticketflow.notification_service.delivery.infrastructure.in.event;

import com.ticketflow.notification_service.delivery.application.usecase.ProcessWelcomeNotificationUseCase;
import com.ticketflow.notification_service.delivery.infrastructure.persistence.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.stereotype.Component;

/**
 * Inbound AMQP adapter — listens on {@code user.registered.queue} and
 * delegates to {@link ProcessWelcomeNotificationUseCase}.
 * <p>
 * Implements idempotent processing: messages already processed are skipped.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredEventListener {

    private final ProcessWelcomeNotificationUseCase processWelcomeNotificationUseCase;
    private final IdempotencyService idempotencyService;
    private final Jackson2JsonMessageConverter messageConverter;

    @RabbitListener(queues = "user.registered.queue")
    public void onUserRegistered(Message rawMessage) {
        String messageId = rawMessage.getMessageProperties().getMessageId();

        if (messageId != null && idempotencyService.isAlreadyProcessed(messageId)) {
            log.info("Skipping duplicate UserRegisteredMessage — messageId: {}", messageId);
            return;
        }

        UserRegisteredMessage message = (UserRegisteredMessage)
                messageConverter.fromMessage(rawMessage);

        log.info("Received UserRegisteredMessage — userId: {}", message.userId());
        processWelcomeNotificationUseCase.execute(message.userId(), message.email());

        if (messageId != null) {
            idempotencyService.markAsProcessed(messageId);
        }
    }
}
