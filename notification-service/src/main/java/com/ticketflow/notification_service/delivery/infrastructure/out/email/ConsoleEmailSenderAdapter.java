package com.ticketflow.notification_service.delivery.infrastructure.out.email;

import com.ticketflow.notification_service.delivery.application.port.out.EmailSenderPort;
import com.ticketflow.notification_service.delivery.domain.Notification;
import lombok.extern.slf4j.Slf4j;

/**
 * Email adapter that logs notifications to the console via SLF4J.
 * <p>
 * Kept as a reference/fallback. The active adapter is {@link JavaMailEmailSenderAdapter}.
 * </p>
 */
@Slf4j
public class ConsoleEmailSenderAdapter implements EmailSenderPort {

    @Override
    public void sendEmail(Notification notification) {
        log.info("[EMAIL] To: {} | Message: {}", notification.userId(), notification.message());
    }
}
