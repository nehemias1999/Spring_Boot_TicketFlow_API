package com.ticketflow.notification_service.delivery.application.usecase;

import com.ticketflow.notification_service.delivery.application.port.out.EmailSenderPort;
import com.ticketflow.notification_service.delivery.domain.Notification;

import java.util.UUID;

/**
 * Use case for processing a ticket-purchased notification.
 * <p>
 * Plain Java class — no Spring annotations. Wired as a {@code @Bean}
 * in {@code RabbitMQConfig}.
 * </p>
 */
public class ProcessNotificationUseCase {

    private final EmailSenderPort emailSenderPort;

    public ProcessNotificationUseCase(EmailSenderPort emailSenderPort) {
        this.emailSenderPort = emailSenderPort;
    }

    public void execute(String ticketId, String userId) {
        String message = "Your ticket " + ticketId + " has been confirmed";
        Notification notification = new Notification(
                UUID.randomUUID().toString(), ticketId, userId, message, "SENT");
        emailSenderPort.sendEmail(notification.userId(), notification.message());
    }
}
