package com.ticketflow.notification_service.delivery.infrastructure.config;

import com.ticketflow.notification_service.delivery.application.port.out.EmailSenderPort;
import com.ticketflow.notification_service.delivery.application.usecase.ProcessNotificationUseCase;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ infrastructure configuration for notification-service.
 * <p>
 * Declares the exchange, queue, and binding topology.
 * Also wires {@link ProcessNotificationUseCase} as a plain bean (no @Service)
 * and configures the {@link SimpleRabbitListenerContainerFactory} with
 * {@link Jackson2JsonMessageConverter} so {@code @RabbitListener} can
 * deserialize JSON payloads into record DTOs.
 * </p>
 */
@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange ticketflowEventsExchange() {
        return new TopicExchange("ticketflow.events", true, false);
    }

    @Bean
    public Queue ticketPurchasedQueue() {
        return new Queue("ticket.purchased.queue", true);
    }

    @Bean
    public Binding ticketPurchasedBinding(Queue ticketPurchasedQueue, TopicExchange ticketflowEventsExchange) {
        return BindingBuilder.bind(ticketPurchasedQueue)
                .to(ticketflowEventsExchange)
                .with("ticket.purchased");
    }

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

    @Bean
    public ProcessNotificationUseCase processNotificationUseCase(EmailSenderPort emailSenderPort) {
        return new ProcessNotificationUseCase(emailSenderPort);
    }
}
