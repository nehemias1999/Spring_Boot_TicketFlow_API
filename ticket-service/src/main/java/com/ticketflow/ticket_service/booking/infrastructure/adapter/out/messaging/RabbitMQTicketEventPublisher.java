package com.ticketflow.ticket_service.booking.infrastructure.adapter.out.messaging;

import com.ticketflow.ticket_service.booking.application.event.TicketPurchasedEvent;
import com.ticketflow.ticket_service.booking.domain.port.out.ITicketEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * AMQP adapter implementing {@link ITicketEventPublisher}.
 * <p>
 * Publishes {@link TicketPurchasedEvent} to the {@code ticketflow.events} topic exchange
 * with routing key {@code ticket.purchased}.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMQTicketEventPublisher implements ITicketEventPublisher {

    static final String EXCHANGE = "ticketflow.events";
    static final String ROUTING_KEY = "ticket.purchased";

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishTicketPurchased(String ticketId, String userId) {
        TicketPurchasedEvent event = new TicketPurchasedEvent(ticketId, userId);
        log.info("Publishing TicketPurchasedEvent — ticketId: {}, userId: {}", ticketId, userId);
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event);
    }
}
