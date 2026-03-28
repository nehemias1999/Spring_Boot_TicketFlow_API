package com.ticketflow.notification_service.delivery.application.usecase;

import com.ticketflow.notification_service.delivery.application.port.out.EmailSenderPort;
import com.ticketflow.notification_service.delivery.domain.Notification;

import java.util.UUID;

/**
 * Use case for processing a ticket-cancelled notification.
 * <p>
 * Plain Java class — no Spring annotations. Wired as a {@code @Bean}
 * in {@code RabbitMQConfig}.
 * </p>
 */
public class ProcessTicketCancelledNotificationUseCase {

    private final EmailSenderPort emailSenderPort;

    public ProcessTicketCancelledNotificationUseCase(EmailSenderPort emailSenderPort) {
        this.emailSenderPort = emailSenderPort;
    }

    public void execute(String ticketId, String userId, String userEmail) {
        String message = "Your ticket " + ticketId + " has been cancelled";
        Notification notification = new Notification(
                UUID.randomUUID().toString(), ticketId, userId, userEmail, message, "SENT");
        emailSenderPort.sendEmail(notification);
    }
}
