package com.ticketflow.ticket_service.booking.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for purchasing a new ticket.
 * The ticket ID is generated server-side as a UUID.
 *
 * @param eventId the ID of the event to purchase a ticket for
 * @param userId  the ID of the user purchasing the ticket
 * @author TicketFlow Team
 */
public record CreateTicketRequest(

        @NotBlank
        @Size(max = 36)
        String eventId,

        @NotBlank
        @Size(max = 50)
        String userId
) {
}
