package com.ticketflow.ticket_service.booking.infrastructure.adapter.out.messaging;

import com.ticketflow.ticket_service.booking.application.event.TicketPurchasedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RabbitMQTicketEventPublisher — unit tests")
class RabbitMQTicketEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private RabbitMQTicketEventPublisher publisher;

    @Test
    @DisplayName("should send message to correct exchange and routing key with correct payload")
    void publishTicketPurchased_sendsCorrectMessage() {
        // given
        String ticketId = "ticket-123";
        String userId = "user-456";

        ArgumentCaptor<String> exchangeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);

        // when
        publisher.publishTicketPurchased(ticketId, userId);

        // then
        verify(rabbitTemplate).convertAndSend(
                exchangeCaptor.capture(),
                routingKeyCaptor.capture(),
                payloadCaptor.capture()
        );

        assertThat(exchangeCaptor.getValue()).isEqualTo(RabbitMQTicketEventPublisher.EXCHANGE);
        assertThat(routingKeyCaptor.getValue()).isEqualTo(RabbitMQTicketEventPublisher.ROUTING_KEY);

        TicketPurchasedEvent event = (TicketPurchasedEvent) payloadCaptor.getValue();
        assertThat(event.ticketId()).isEqualTo(ticketId);
        assertThat(event.userId()).isEqualTo(userId);
    }
}
