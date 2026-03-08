package com.ticketflow.event_service.catalog.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO representing a catalog entry returned to the client.
 * <p>
 * This record contains all publicly visible fields of a catalog entry,
 * excluding internal flags such as the soft-delete marker.
 * </p>
 *
 * @param id          unique business identifier (e.g., "EVT-001")
 * @param title       name of the event
 * @param description short summary of the event
 * @param date        date and time of the event as a string
 * @param location    venue where the event takes place
 * @param basePrice   base reference price
 * @param createdAt   timestamp when the entry was created
 * @param updatedAt   timestamp when the entry was last updated
 * @author TicketFlow Team
 */
public record CatalogResponse(
        String id,
        String title,
        String description,
        String date,
        String location,
        BigDecimal basePrice,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

