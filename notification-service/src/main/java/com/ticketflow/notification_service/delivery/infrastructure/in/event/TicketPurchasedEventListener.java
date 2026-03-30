package com.ticketflow.notification_service.delivery.infrastructure.in.event;

import com.ticketflow.notification_service.delivery.application.usecase.ProcessNotificationUseCase;
import com.ticketflow.notification_service.delivery.infrastructure.persistence.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.stereotype.Component;

/**
 * Inbound AMQP adapter — listens on {@code ticket.purchased.queue} and
 * delegates to {@link ProcessNotificationUseCase}.
 * <p>
 * Implements idempotent processing: messages already processed are skipped.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketPurchasedEventListener {

    private final ProcessNotificationUseCase processNotificationUseCase;
    private final IdempotencyService idempotencyService;
    private final Jackson2JsonMessageConverter messageConverter;

    @RabbitListener(queues = "ticket.purchased.queue")
    public void onTicketPurchased(Message rawMessage) {
        String messageId = rawMessage.getMessageProperties().getMessageId();

        if (messageId != null && idempotencyService.isAlreadyProcessed(messageId)) {
            log.info("Skipping duplicate TicketPurchasedMessage — messageId: {}", messageId);
            return;
        }

        TicketPurchasedMessage message = (TicketPurchasedMessage)
                messageConverter.fromMessage(rawMessage);

        log.info("Received TicketPurchasedMessage — ticketId: {}, userId: {}",
                message.ticketId(), message.userId());

        processNotificationUseCase.execute(message.ticketId(), message.userId(), message.userEmail());

        if (messageId != null) {
            idempotencyService.markAsProcessed(messageId);
        }
    }
}
