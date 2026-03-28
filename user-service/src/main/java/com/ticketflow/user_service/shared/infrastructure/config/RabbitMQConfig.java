package com.ticketflow.user_service.shared.infrastructure.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ infrastructure configuration for user-service.
 * <p>
 * Declares the {@code ticketflow.events} topic exchange, registers a
 * {@link Jackson2JsonMessageConverter}, and configures the {@link RabbitTemplate}
 * to use it so events are published as JSON.
 * </p>
 */
@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange ticketflowEventsExchange() {
        return new TopicExchange("ticketflow.events", true, false);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
