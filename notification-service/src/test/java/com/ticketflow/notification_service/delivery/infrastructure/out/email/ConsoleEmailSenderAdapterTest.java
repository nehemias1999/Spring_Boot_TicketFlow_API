package com.ticketflow.notification_service.delivery.infrastructure.out.email;

import com.ticketflow.notification_service.delivery.domain.Notification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("ConsoleEmailSenderAdapter — unit tests")
class ConsoleEmailSenderAdapterTest {

    @Test
    @DisplayName("should log email without throwing any exception")
    void sendEmail_doesNotThrow() {
        ConsoleEmailSenderAdapter adapter = new ConsoleEmailSenderAdapter();
        Notification notification = new Notification("notif-001", "ticket-123", "user-001",
                "user@test.com", "Your ticket ticket-123 has been confirmed", "SENT");
        assertThatCode(() -> adapter.sendEmail(notification))
                .doesNotThrowAnyException();
    }
}
