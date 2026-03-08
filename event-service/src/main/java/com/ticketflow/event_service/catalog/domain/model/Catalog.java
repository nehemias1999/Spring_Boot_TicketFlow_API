package com.ticketflow.event_service.catalog.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Core domain model representing an event catalog entry.
 * <p>
 * This is a pure domain object with no infrastructure dependencies.
 * It holds all business-relevant attributes for an event that can be
 * listed in the ticket reservation system's catalog.
 * </p>
 *
 * @author TicketFlow Team
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Catalog {

    /**
     * Unique business identifier for the catalog entry (e.g., "EVT-001").
     * This ID is provided by the client and used across microservices.
     */
    private String id;

    /**
     * Title or name of the event (e.g., "Lollapalooza 2026").
     */
    private String title;

    /**
     * Short description summarizing the event.
     */
    private String description;

    /**
     * Date and time of the event as a human-readable string (e.g., "2026-10-15 20:00").
     */
    private String date;

    /**
     * Venue or location where the event takes place (e.g., "Estadio River Plate").
     */
    private String location;

    /**
     * Base reference price for display purposes.
     */
    private BigDecimal basePrice;

    /**
     * Soft-delete flag. When {@code true}, the catalog entry is considered deleted.
     */
    @Builder.Default
    private boolean deleted = false;

    /**
     * Timestamp indicating when this catalog entry was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp indicating when this catalog entry was last updated.
     */
    private LocalDateTime updatedAt;
}

