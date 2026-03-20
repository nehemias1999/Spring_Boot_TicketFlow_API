package com.ticketflow.notification_service.delivery.infrastructure.out.email;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("ConsoleEmailSenderAdapter — unit tests")
class ConsoleEmailSenderAdapterTest {

    @Test
    @DisplayName("should log email without throwing any exception")
    void sendEmail_doesNotThrow() {
        ConsoleEmailSenderAdapter adapter = new ConsoleEmailSenderAdapter();
        assertThatCode(() -> adapter.sendEmail("user-001", "Your ticket ticket-123 has been confirmed"))
                .doesNotThrowAnyException();
    }
}
