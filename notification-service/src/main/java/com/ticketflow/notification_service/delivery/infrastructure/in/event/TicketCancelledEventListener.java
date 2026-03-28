package com.ticketflow.notification_service.delivery.infrastructure.in.event;

import com.ticketflow.notification_service.delivery.application.usecase.ProcessTicketCancelledNotificationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Inbound AMQP adapter — listens on {@code ticket.cancelled.queue} and
 * delegates to {@link ProcessTicketCancelledNotificationUseCase}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketCancelledEventListener {

    private final ProcessTicketCancelledNotificationUseCase processTicketCancelledNotificationUseCase;

    @RabbitListener(queues = "ticket.cancelled.queue")
    public void onTicketCancelled(TicketCancelledMessage message) {
        log.info("Received TicketCancelledMessage — ticketId: {}, userId: {}",
                message.ticketId(), message.userId());
        processTicketCancelledNotificationUseCase.execute(message.ticketId(), message.userId(), message.userEmail());
    }
}
