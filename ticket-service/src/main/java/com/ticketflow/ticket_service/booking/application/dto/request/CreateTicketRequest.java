package com.ticketflow.ticket_service.booking.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for purchasing a new ticket.
 * The ticket ID is generated server-side as a UUID.
 * The userId is extracted from the authenticated user's JWT token (X-User-Id header).
 *
 * @param eventId the ID of the event to purchase a ticket for
 * @author TicketFlow Team
 */
public record CreateTicketRequest(

        @NotBlank
        @Size(max = 36)
        String eventId
) {
}
