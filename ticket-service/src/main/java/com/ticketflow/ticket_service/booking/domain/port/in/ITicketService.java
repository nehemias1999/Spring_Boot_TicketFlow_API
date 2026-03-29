package com.ticketflow.ticket_service.booking.domain.port.in;

import com.ticketflow.ticket_service.booking.application.dto.request.CreateTicketRequest;
import com.ticketflow.ticket_service.booking.application.dto.response.TicketResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Inbound port defining the use cases available for ticket management.
 *
 * @author TicketFlow Team
 */
public interface ITicketService {

    TicketResponse create(CreateTicketRequest request, String authenticatedUserId, String userEmail, String userRole);

    TicketResponse getById(String id, String authenticatedUserId, String userRole);

    Page<TicketResponse> getAll(String eventId, String status, Pageable pageable, String authenticatedUserId, String userRole);

    TicketResponse cancel(String id, String authenticatedUserId, String userEmail, String userRole);

    void delete(String id, String authenticatedUserId, String userRole);
}
