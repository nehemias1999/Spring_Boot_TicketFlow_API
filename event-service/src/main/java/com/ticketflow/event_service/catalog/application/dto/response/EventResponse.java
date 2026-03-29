package com.ticketflow.event_service.catalog.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO representing an event entry returned to the client.
 *
 * @param id        unique business identifier
 * @param title     name of the event
 * @param description short summary of the event
 * @param date      date and time of the event as a string
 * @param location  venue where the event takes place
 * @param basePrice base reference price
 * @param creatorId the user ID of the SELLER who created the event (may be null)
 * @param createdAt timestamp when the entry was created
 * @param updatedAt timestamp when the entry was last updated
 * @author TicketFlow Team
 */
public record EventResponse(
        String id,
        String title,
        String description,
        String date,
        String location,
        BigDecimal basePrice,
        String creatorId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
