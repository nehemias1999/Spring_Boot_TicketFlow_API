package com.ticketflow.notification_service.delivery.application.port.out;

import com.ticketflow.notification_service.delivery.domain.Notification;

/**
 * Outbound port for sending email notifications.
 * <p>
 * Pure Java interface — no Spring dependencies.
 * </p>
 */
public interface EmailSenderPort {

    void sendEmail(Notification notification);
}
