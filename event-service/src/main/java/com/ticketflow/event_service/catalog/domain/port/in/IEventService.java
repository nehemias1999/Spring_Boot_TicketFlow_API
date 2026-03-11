package com.ticketflow.event_service.catalog.domain.port.in;

import com.ticketflow.event_service.catalog.application.dto.response.EventResponse;
import com.ticketflow.event_service.catalog.application.dto.request.CreateEventRequest;
import com.ticketflow.event_service.catalog.application.dto.request.UpdateEventRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Inbound port defining the use cases available for event management.
 * <p>
 * This interface is implemented by the application service layer and
 * consumed by the infrastructure adapters (e.g., REST controller).
 * </p>
 *
 * @author TicketFlow Team
 */
public interface IEventService {

    /**
     * Creates a new event entry in the system.
     *
     * @param request the data required to create an event entry
     * @return the created event as a response DTO
     * @throws com.ticketflow.event_service.catalog.domain.exception.EventAlreadyExistsException
     *         if an event with the same ID already exists
     */
    EventResponse create(CreateEventRequest request);

    /**
     * Retrieves a single event entry by its unique ID.
     *
     * @param id the unique business identifier of the event
     * @return the event data as a response DTO
     * @throws com.ticketflow.event_service.catalog.domain.exception.EventNotFoundException
     *         if no active event with the given ID exists
     */
    EventResponse getById(String id);

    /**
     * Retrieves a paginated list of all active (non-deleted) event entries.
     *
     * @param pageable pagination and sorting parameters
     * @return a page of event response DTOs
     */
    Page<EventResponse> getAll(Pageable pageable);

    /**
     * Updates an existing event entry with the provided data.
     *
     * @param id      the unique business identifier of the event to update
     * @param request the data to apply as updates
     * @return the updated event as a response DTO
     * @throws com.ticketflow.event_service.catalog.domain.exception.EventNotFoundException
     *         if no active event with the given ID exists
     */
    EventResponse update(String id, UpdateEventRequest request);

    /**
     * Performs a soft delete on the event entry with the given ID.
     * <p>
     * The record is not physically removed; instead, it is marked as deleted.
     * </p>
     *
     * @param id the unique business identifier of the event to delete
     * @throws com.ticketflow.event_service.catalog.domain.exception.EventNotFoundException
     *         if no active event with the given ID exists
     */
    void delete(String id);
}

