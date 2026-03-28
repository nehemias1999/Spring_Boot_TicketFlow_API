package com.ticketflow.notification_service.delivery.infrastructure.in.event;

import com.ticketflow.notification_service.delivery.application.usecase.ProcessNotificationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Inbound AMQP adapter — listens on {@code ticket.purchased.queue} and
 * delegates to {@link ProcessNotificationUseCase}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketPurchasedEventListener {

    private final ProcessNotificationUseCase processNotificationUseCase;

    @RabbitListener(queues = "ticket.purchased.queue")
    public void onTicketPurchased(TicketPurchasedMessage message) {
        log.info("Received TicketPurchasedMessage — ticketId: {}, userId: {}",
                message.ticketId(), message.userId());
        processNotificationUseCase.execute(message.ticketId(), message.userId(), message.userEmail());
    }
}
