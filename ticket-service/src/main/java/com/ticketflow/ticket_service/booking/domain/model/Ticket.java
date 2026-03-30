package com.ticketflow.ticket_service.booking.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Core domain model representing a ticket booking.
 * <p>
 * This is a pure domain object with no infrastructure dependencies.
 * It holds all business-relevant attributes for a ticket purchased by a user
 * for a specific event.
 * </p>
 *
 * @author TicketFlow Team
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

    /**
     * Unique business identifier for the ticket (e.g., "TKT-001").
     * This ID is provided by the client and used across microservices.
     */
    private String id;

    /**
     * Reference to the event this ticket belongs to (e.g., "EVT-001").
     */
    private String eventId;

    /**
     * Reference to the user who purchased this ticket.
     */
    private String userId;

    /**
     * Date and time when the ticket was purchased.
     * Auto-set to {@code LocalDateTime.now()} on creation.
     */
    private LocalDateTime purchaseDate;

    /**
     * Current lifecycle status of the ticket.
     * Defaults to {@link TicketStatus#CONFIRMED} on creation.
     */
    private TicketStatus status;

    /**
     * Price paid at the time of purchase (copied from event's basePrice).
     */
    private BigDecimal price;

    /**
     * Soft-delete flag. When {@code true}, the ticket is considered deleted.
     */
    @Builder.Default
    private boolean deleted = false;

    /**
     * Timestamp indicating when this ticket was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp indicating when this ticket was last updated.
     */
    private LocalDateTime updatedAt;
}
