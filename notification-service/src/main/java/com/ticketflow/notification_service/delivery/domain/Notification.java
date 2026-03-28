package com.ticketflow.notification_service.delivery.domain;

/**
 * Domain record representing a notification to be delivered.
 *
 * @param id             unique notification identifier
 * @param ticketId       the ticket that triggered this notification
 * @param userId         the recipient user ID
 * @param recipientEmail the recipient email address
 * @param message        the notification message text
 * @param status         delivery status (e.g. "SENT")
 */
public record Notification(String id, String ticketId, String userId, String recipientEmail, String message, String status) {
}
