package com.ticketflow.ticket_service.booking.infrastructure.adapter.out.messaging;

import com.ticketflow.ticket_service.booking.application.event.TicketCancelledEvent;
import com.ticketflow.ticket_service.booking.application.event.TicketPurchasedEvent;
import com.ticketflow.ticket_service.booking.domain.port.out.ITicketEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * AMQP adapter implementing {@link ITicketEventPublisher}.
 * <p>
 * Sets a unique {@code messageId} on every message to enable idempotent
 * processing in notification-service.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMQTicketEventPublisher implements ITicketEventPublisher {

    static final String EXCHANGE = "ticketflow.events";
    static final String ROUTING_KEY = "ticket.purchased";
    static final String ROUTING_KEY_CANCELLED = "ticket.cancelled";

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishTicketPurchased(String ticketId, String userId, String userEmail) {
        TicketPurchasedEvent event = new TicketPurchasedEvent(ticketId, userId, userEmail);
        String messageId = UUID.randomUUID().toString();
        log.info("Publishing TicketPurchasedEvent — ticketId: {}, messageId: {}", ticketId, messageId);
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event, msg -> {
            msg.getMessageProperties().setMessageId(messageId);
            return msg;
        });
    }

    @Override
    public void publishTicketCancelled(String ticketId, String userId, String userEmail) {
        TicketCancelledEvent event = new TicketCancelledEvent(ticketId, userId, userEmail);
        String messageId = UUID.randomUUID().toString();
        log.info("Publishing TicketCancelledEvent — ticketId: {}, messageId: {}", ticketId, messageId);
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY_CANCELLED, event, msg -> {
            msg.getMessageProperties().setMessageId(messageId);
            return msg;
        });
    }
}
