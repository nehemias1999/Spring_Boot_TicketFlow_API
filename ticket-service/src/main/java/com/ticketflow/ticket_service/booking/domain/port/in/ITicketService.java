package com.ticketflow.ticket_service.booking.domain.port.in;

import com.ticketflow.ticket_service.booking.application.dto.request.CreateTicketRequest;
import com.ticketflow.ticket_service.booking.application.dto.response.TicketResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Inbound port defining the use cases available for ticket management.
 * <p>
 * Implemented by the application service and called by inbound adapters
 * (e.g., REST controllers).
 * </p>
 *
 * @author TicketFlow Team
 */
public interface ITicketService {

    /**
     * Purchases a new ticket for the authenticated user.
     *
     * @param request           the creation request containing event details
     * @param authenticatedUserId the ID of the authenticated user (from X-User-Id header)
     * @param userEmail         the email of the authenticated user (from X-User-Email header)
     * @return the created ticket response
     */
    TicketResponse create(CreateTicketRequest request, String authenticatedUserId, String userEmail);

    /**
     * Retrieves a ticket by its unique ID.
     * The authenticated user must be the owner of the ticket.
     *
     * @param id                  the ticket identifier
     * @param authenticatedUserId the ID of the authenticated user (from X-User-Id header)
     * @return the ticket response
     */
    TicketResponse getById(String id, String authenticatedUserId);

    /**
     * Retrieves a paginated and filtered list of tickets belonging to the authenticated user.
     *
     * @param eventId             optional filter by event ID
     * @param status              optional filter by ticket status
     * @param pageable            pagination and sorting parameters
     * @param authenticatedUserId the ID of the authenticated user (from X-User-Id header)
     * @return a page of ticket responses
     */
    Page<TicketResponse> getAll(String eventId, String status, Pageable pageable, String authenticatedUserId);

    /**
     * Cancels an active ticket.
     * The authenticated user must be the current owner of the ticket.
     *
     * @param id                  the ticket identifier
     * @param authenticatedUserId the ID of the authenticated user (from X-User-Id header)
     * @return the cancelled ticket response
     */
    TicketResponse cancel(String id, String authenticatedUserId);

    /**
     * Soft-deletes a ticket by its unique ID.
     * The authenticated user must be the current owner of the ticket.
     *
     * @param id                  the ticket identifier
     * @param authenticatedUserId the ID of the authenticated user (from X-User-Id header)
     */
    void delete(String id, String authenticatedUserId);
}
