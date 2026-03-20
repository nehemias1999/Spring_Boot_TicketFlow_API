package com.ticketflow.ticket_service;

import com.ticketflow.ticket_service.booking.domain.port.out.ITicketEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Integration test that verifies the full Spring application context loads
 * successfully with all beans, configurations, and auto-wired dependencies.
 * <p>
 * Uses the {@code test} property source (src/test/resources/application.properties)
 * which disables Spring Cloud Config Server and Eureka connections, and
 * replaces MySQL with an in-memory H2 database to allow context loading
 * without any external infrastructure running.
 * </p>
 */
@SpringBootTest
@DisplayName("TicketService — application context integration test")
class TicketServiceApplicationTests {

    @MockBean
    private ITicketEventPublisher ticketEventPublisher;


    /**
     * Validates that the Spring application context starts without errors,
     * ensuring all beans are properly wired, JPA entities are mapped, and
     * configuration is valid.
     */
    @Test
    @DisplayName("should load the application context without errors")
    void contextLoads() {
    }
}
