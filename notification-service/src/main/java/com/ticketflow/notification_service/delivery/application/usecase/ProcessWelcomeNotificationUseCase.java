package com.ticketflow.notification_service.delivery.application.usecase;

import com.ticketflow.notification_service.delivery.application.port.out.EmailSenderPort;
import com.ticketflow.notification_service.delivery.domain.Notification;

import java.util.UUID;

/**
 * Use case for sending a welcome notification when a new user registers.
 * <p>
 * Plain Java class — no Spring annotations. Wired as a {@code @Bean}
 * in {@code RabbitMQConfig}.
 * </p>
 */
public class ProcessWelcomeNotificationUseCase {

    private final EmailSenderPort emailSenderPort;

    public ProcessWelcomeNotificationUseCase(EmailSenderPort emailSenderPort) {
        this.emailSenderPort = emailSenderPort;
    }

    public void execute(String userId, String email) {
        String message = "Welcome to TicketFlow! Your account has been created successfully.";
        Notification notification = new Notification(
                UUID.randomUUID().toString(), null, userId, email, message, "SENT");
        emailSenderPort.sendEmail(notification);
    }
}
