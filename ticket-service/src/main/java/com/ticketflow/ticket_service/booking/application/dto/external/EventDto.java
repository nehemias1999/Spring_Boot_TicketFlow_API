package com.ticketflow.ticket_service.booking.application.dto.external;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing the event data fetched from event-service.
 *
 * @author TicketFlow Team
 */
public record EventDto(
        String id,
        String title,
        String description,
        LocalDateTime date,
        String location,
        BigDecimal basePrice,
        int capacity,
        int availableTickets,
        String creatorId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
