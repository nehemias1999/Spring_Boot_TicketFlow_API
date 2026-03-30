package com.ticketflow.ticket_service.booking.application.dto.response;

import com.ticketflow.ticket_service.booking.domain.model.TicketStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO representing a ticket booking.
 *
 * @param id           the unique business identifier
 * @param eventId      the event this ticket belongs to
 * @param userId       the user who owns this ticket
 * @param purchaseDate the date and time the ticket was purchased
 * @param status       the current lifecycle status of the ticket
 * @param price        the price paid at time of purchase
 * @param createdAt    the timestamp when this record was created
 * @param updatedAt    the timestamp when this record was last updated
 * @author TicketFlow Team
 */
public record TicketResponse(
        String id,
        String eventId,
        String userId,
        LocalDateTime purchaseDate,
        TicketStatus status,
        BigDecimal price,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
