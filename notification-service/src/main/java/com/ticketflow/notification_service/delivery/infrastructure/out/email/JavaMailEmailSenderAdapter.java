package com.ticketflow.notification_service.delivery.infrastructure.out.email;

import com.ticketflow.notification_service.delivery.application.port.out.EmailSenderPort;
import com.ticketflow.notification_service.delivery.domain.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Email adapter that sends notifications via SMTP using {@link JavaMailSender}.
 * <p>
 * The recipient address is the {@code userId} field, which in this project
 * is assumed to be the user's email address. The sender address is
 * configurable via {@code notification.mail.from}.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JavaMailEmailSenderAdapter implements EmailSenderPort {

    private final JavaMailSender mailSender;

    @Value("${notification.mail.from}")
    private String from;

    @Override
    public void sendEmail(Notification notification) {
        log.info("Sending email to: {} for ticketId: {}", notification.recipientEmail(), notification.ticketId());

        String subject;
        if (notification.ticketId() == null) {
            subject = "TicketFlow — Welcome to TicketFlow!";
        } else if (notification.message().contains("cancelled")) {
            subject = "TicketFlow — Your ticket has been cancelled";
        } else {
            subject = "TicketFlow — Your ticket has been confirmed";
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(notification.recipientEmail());
        message.setSubject(subject);
        message.setText(notification.message());

        mailSender.send(message);
        log.info("Email sent successfully to: {}", notification.recipientEmail());
    }
}
