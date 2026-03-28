package com.ticketflow.notification_service.delivery.infrastructure.in.event;

import com.ticketflow.notification_service.delivery.application.usecase.ProcessNotificationUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketPurchasedEventListener — unit tests")
class TicketPurchasedEventListenerTest {

    @Mock
    private ProcessNotificationUseCase processNotificationUseCase;

    @InjectMocks
    private TicketPurchasedEventListener listener;

    @Test
    @DisplayName("should delegate to ProcessNotificationUseCase with correct ticketId and userId")
    void onTicketPurchased_delegatesToUseCase() {
        // given
        TicketPurchasedMessage message = new TicketPurchasedMessage("ticket-001", "user-001", "user@test.com");

        // when
        listener.onTicketPurchased(message);

        // then
        verify(processNotificationUseCase).execute("ticket-001", "user-001", "user@test.com");
    }
}
