package com.ticketflow.notification_service.delivery.application.usecase;

import com.ticketflow.notification_service.delivery.application.port.out.EmailSenderPort;
import com.ticketflow.notification_service.delivery.domain.Notification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessNotificationUseCase — unit tests")
class ProcessNotificationUseCaseTest {

    @Mock
    private EmailSenderPort emailSenderPort;

    @Test
    @DisplayName("should send email to correct userId with message containing ticketId")
    void execute_sendsEmailWithCorrectPayload() {
        // given
        ProcessNotificationUseCase useCase = new ProcessNotificationUseCase(emailSenderPort);
        String ticketId = "ticket-abc";
        String userId = "user-xyz";
        String userEmail = "user@test.com";

        // when
        useCase.execute(ticketId, userId, userEmail);

        // then
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(emailSenderPort).sendEmail(captor.capture());

        assertThat(captor.getValue().userId()).isEqualTo(userId);
        assertThat(captor.getValue().recipientEmail()).isEqualTo(userEmail);
        assertThat(captor.getValue().message()).contains(ticketId);
    }
}
