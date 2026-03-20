package com.ticketflow.notification_service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Verifies the full Spring application context loads successfully without
 * a live RabbitMQ broker. The {@link ConnectionFactory} is mocked so AMQP
 * auto-configuration does not attempt a real TCP connection.
 */
@SpringBootTest
@DisplayName("NotificationService — application context integration test")
class NotificationServiceApplicationTests {

    @MockBean
    private org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory;

    @Test
    @DisplayName("should load the application context without errors")
    void contextLoads() {
    }
}
