package com.ticketflow.user_service.shared.infrastructure.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ infrastructure configuration for user-service.
 * <p>
 * Declares the {@code ticketflow.events} topic exchange and registers a
 * {@link Jackson2JsonMessageConverter} so events are serialized as JSON.
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
}
