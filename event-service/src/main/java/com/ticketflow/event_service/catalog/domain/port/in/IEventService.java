package com.ticketflow.event_service.catalog.domain.port.in;

import com.ticketflow.event_service.catalog.application.dto.response.EventResponse;
import com.ticketflow.event_service.catalog.application.dto.request.CreateEventRequest;
import com.ticketflow.event_service.catalog.application.dto.request.UpdateEventRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Inbound port defining the use cases available for event management.
 *
 * @author TicketFlow Team
 */
public interface IEventService {

    EventResponse create(CreateEventRequest request, String creatorId, String role);

    EventResponse getById(String id);

    Page<EventResponse> getAll(String title, String location, Pageable pageable);

    EventResponse update(String id, UpdateEventRequest request, String requestingUserId, String role);

    void delete(String id, String requestingUserId, String role);

    Page<EventResponse> getMyEvents(String creatorId, Pageable pageable);

    List<String> getMyEventIds(String creatorId);
}
