package com.ticketflow.notification_service.delivery.application.port.out;

/**
 * Outbound port for sending email notifications.
 * <p>
 * Pure Java interface — no Spring dependencies.
 * </p>
 */
public interface EmailSenderPort {

    void sendEmail(String userId, String message);
}
