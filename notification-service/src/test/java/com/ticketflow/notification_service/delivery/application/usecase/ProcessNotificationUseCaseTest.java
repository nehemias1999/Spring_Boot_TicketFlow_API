package com.ticketflow.notification_service.delivery.application.usecase;

import com.ticketflow.notification_service.delivery.application.port.out.EmailSenderPort;
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

        // when
        useCase.execute(ticketId, userId);

        // then
        ArgumentCaptor<String> userCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSenderPort).sendEmail(userCaptor.capture(), messageCaptor.capture());

        assertThat(userCaptor.getValue()).isEqualTo(userId);
        assertThat(messageCaptor.getValue()).contains(ticketId);
    }
}
