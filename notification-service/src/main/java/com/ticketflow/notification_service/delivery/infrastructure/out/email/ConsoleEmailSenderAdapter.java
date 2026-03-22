package com.ticketflow.notification_service.delivery.infrastructure.out.email;

import com.ticketflow.notification_service.delivery.application.port.out.EmailSenderPort;
import com.ticketflow.notification_service.delivery.domain.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Email adapter that logs notifications to the console via SLF4J.
 * <p>
 * In production this would be replaced by an SMTP or email-service integration.
 * </p>
 */
@Slf4j
@Component
public class ConsoleEmailSenderAdapter implements EmailSenderPort {

    @Override
    public void sendEmail(Notification notification) {
        log.info("[EMAIL] To: {} | Message: {}", notification.userId(), notification.message());
    }
}
