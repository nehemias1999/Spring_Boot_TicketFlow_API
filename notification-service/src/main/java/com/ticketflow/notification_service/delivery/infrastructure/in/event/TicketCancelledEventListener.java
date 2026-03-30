package com.ticketflow.notification_service.delivery.infrastructure.in.event;

import com.ticketflow.notification_service.delivery.application.usecase.ProcessTicketCancelledNotificationUseCase;
import com.ticketflow.notification_service.delivery.infrastructure.persistence.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.stereotype.Component;

/**
 * Inbound AMQP adapter — listens on {@code ticket.cancelled.queue} and
 * delegates to {@link ProcessTicketCancelledNotificationUseCase}.
 * <p>
 * Implements idempotent processing: messages already processed are skipped.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketCancelledEventListener {

    private final ProcessTicketCancelledNotificationUseCase processTicketCancelledNotificationUseCase;
    private final IdempotencyService idempotencyService;
    private final Jackson2JsonMessageConverter messageConverter;

    @RabbitListener(queues = "ticket.cancelled.queue")
    public void onTicketCancelled(Message rawMessage) {
        String messageId = rawMessage.getMessageProperties().getMessageId();

        if (messageId != null && idempotencyService.isAlreadyProcessed(messageId)) {
            log.info("Skipping duplicate TicketCancelledMessage — messageId: {}", messageId);
            return;
        }

        TicketCancelledMessage message = (TicketCancelledMessage)
                messageConverter.fromMessage(rawMessage);

        log.info("Received TicketCancelledMessage — ticketId: {}, userId: {}",
                message.ticketId(), message.userId());

        processTicketCancelledNotificationUseCase.execute(message.ticketId(), message.userId(), message.userEmail());

        if (messageId != null) {
            idempotencyService.markAsProcessed(messageId);
        }
    }
}
