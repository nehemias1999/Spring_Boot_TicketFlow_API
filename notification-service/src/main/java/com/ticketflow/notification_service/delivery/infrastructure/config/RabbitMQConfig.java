package com.ticketflow.notification_service.delivery.infrastructure.config;

import com.ticketflow.notification_service.delivery.application.port.out.EmailSenderPort;
import com.ticketflow.notification_service.delivery.application.usecase.ProcessNotificationUseCase;
import com.ticketflow.notification_service.delivery.application.usecase.ProcessTicketCancelledNotificationUseCase;
import com.ticketflow.notification_service.delivery.application.usecase.ProcessWelcomeNotificationUseCase;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ infrastructure configuration for notification-service.
 * <p>
 * Declares the main exchange, queues (with DLQ arguments), DLQ exchange,
 * and DLQ queues so that failed messages are routed to the dead-letter
 * queues rather than dropped.
 * </p>
 */
@Configuration
public class RabbitMQConfig {

    // ── Main exchange ──────────────────────────────────────────────────────────

    @Bean
    public TopicExchange ticketflowEventsExchange() {
        return new TopicExchange("ticketflow.events", true, false);
    }

    // ── Dead-letter exchange ───────────────────────────────────────────────────

    @Bean
    public TopicExchange ticketflowDlqExchange() {
        return new TopicExchange("ticketflow.events.dlq", true, false);
    }

    // ── Main queues (with DLQ routing) ────────────────────────────────────────

    @Bean
    public Queue ticketPurchasedQueue() {
        return QueueBuilder.durable("ticket.purchased.queue")
                .withArgument("x-dead-letter-exchange", "ticketflow.events.dlq")
                .withArgument("x-dead-letter-routing-key", "ticket.purchased.dlq")
                .build();
    }

    @Bean
    public Binding ticketPurchasedBinding(Queue ticketPurchasedQueue, TopicExchange ticketflowEventsExchange) {
        return BindingBuilder.bind(ticketPurchasedQueue)
                .to(ticketflowEventsExchange)
                .with("ticket.purchased");
    }

    @Bean
    public Queue ticketCancelledQueue() {
        return QueueBuilder.durable("ticket.cancelled.queue")
                .withArgument("x-dead-letter-exchange", "ticketflow.events.dlq")
                .withArgument("x-dead-letter-routing-key", "ticket.cancelled.dlq")
                .build();
    }

    @Bean
    public Binding ticketCancelledBinding(Queue ticketCancelledQueue, TopicExchange ticketflowEventsExchange) {
        return BindingBuilder.bind(ticketCancelledQueue)
                .to(ticketflowEventsExchange)
                .with("ticket.cancelled");
    }

    @Bean
    public Queue userRegisteredQueue() {
        return QueueBuilder.durable("user.registered.queue")
                .withArgument("x-dead-letter-exchange", "ticketflow.events.dlq")
                .withArgument("x-dead-letter-routing-key", "user.registered.dlq")
                .build();
    }

    @Bean
    public Binding userRegisteredBinding(Queue userRegisteredQueue, TopicExchange ticketflowEventsExchange) {
        return BindingBuilder.bind(userRegisteredQueue)
                .to(ticketflowEventsExchange)
                .with("user.registered");
    }

    // ── Dead-letter queues ────────────────────────────────────────────────────

    @Bean
    public Queue ticketPurchasedDlq() {
        return QueueBuilder.durable("ticket.purchased.queue.dlq").build();
    }

    @Bean
    public Binding ticketPurchasedDlqBinding(TopicExchange ticketflowDlqExchange) {
        return BindingBuilder.bind(ticketPurchasedDlq())
                .to(ticketflowDlqExchange)
                .with("ticket.purchased.dlq");
    }

    @Bean
    public Queue ticketCancelledDlq() {
        return QueueBuilder.durable("ticket.cancelled.queue.dlq").build();
    }

    @Bean
    public Binding ticketCancelledDlqBinding(TopicExchange ticketflowDlqExchange) {
        return BindingBuilder.bind(ticketCancelledDlq())
                .to(ticketflowDlqExchange)
                .with("ticket.cancelled.dlq");
    }

    @Bean
    public Queue userRegisteredDlq() {
        return QueueBuilder.durable("user.registered.queue.dlq").build();
    }

    @Bean
    public Binding userRegisteredDlqBinding(TopicExchange ticketflowDlqExchange) {
        return BindingBuilder.bind(userRegisteredDlq())
                .to(ticketflowDlqExchange)
                .with("user.registered.dlq");
    }

    // ── Serialization / Listener factory ─────────────────────────────────────

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, Jackson2JsonMessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        return factory;
    }

    // ── Use-case beans ────────────────────────────────────────────────────────

    @Bean
    public ProcessNotificationUseCase processNotificationUseCase(EmailSenderPort emailSenderPort) {
        return new ProcessNotificationUseCase(emailSenderPort);
    }

    @Bean
    public ProcessWelcomeNotificationUseCase processWelcomeNotificationUseCase(EmailSenderPort emailSenderPort) {
        return new ProcessWelcomeNotificationUseCase(emailSenderPort);
    }

    @Bean
    public ProcessTicketCancelledNotificationUseCase processTicketCancelledNotificationUseCase(EmailSenderPort emailSenderPort) {
        return new ProcessTicketCancelledNotificationUseCase(emailSenderPort);
    }
}
