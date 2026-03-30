package com.ticketflow.event_service.catalog.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO representing an event entry returned to the client.
 *
 * @param id               unique business identifier
 * @param title            name of the event
 * @param description      short summary of the event
 * @param date             date and time of the event
 * @param location         venue where the event takes place
 * @param basePrice        base reference price
 * @param capacity         total number of tickets for the event
 * @param availableTickets current number of tickets available for purchase
 * @param creatorId        the user ID of the SELLER who created the event (may be null)
 * @param createdAt        timestamp when the entry was created
 * @param updatedAt        timestamp when the entry was last updated
 * @author TicketFlow Team
 */
public record EventResponse(
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
