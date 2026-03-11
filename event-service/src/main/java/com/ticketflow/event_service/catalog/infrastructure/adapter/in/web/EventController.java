package com.ticketflow.event_service.catalog.infrastructure.adapter.in.web;

import com.ticketflow.event_service.catalog.application.dto.response.EventResponse;
import com.ticketflow.event_service.catalog.application.dto.request.CreateEventRequest;
import com.ticketflow.event_service.catalog.application.dto.request.UpdateEventRequest;
import com.ticketflow.event_service.catalog.domain.port.in.IEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing CRUD endpoints for event management.
 * <p>
 * This is an inbound adapter in the hexagonal architecture. It receives
 * HTTP requests, delegates to the {@link IEventService}, and returns
 * appropriate HTTP responses.
 * </p>
 * <p>
 * Base path: {@code /api/v1/events}
 * </p>
 *
 * @author TicketFlow Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final IEventService eventServicePort;

    /**
     * Creates a new event entry.
     *
     * @param request the validated creation request body
     * @return the created event with HTTP 201 status
     */
    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        log.info("POST /api/v1/events - Request received to create event with id: {}", request.id());
        EventResponse response = eventServicePort.create(request);
        log.info("POST /api/v1/events - Event created successfully with id: {}", response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves a single event entry by its unique ID.
     *
     * @param id the unique business identifier (path variable)
     * @return the event data with HTTP 200 status
     */
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getById(@PathVariable String id) {
        log.info("GET /api/v1/events/{} - Request received to retrieve event", id);
        EventResponse response = eventServicePort.getById(id);
        log.info("GET /api/v1/events/{} - Event retrieved successfully", id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a paginated list of all active event entries.
     *
     * @param page the page number (zero-based), defaults to 0
     * @param size the number of items per page, defaults to 10
     * @return a page of event entries with HTTP 200 status
     */
    @GetMapping
    public ResponseEntity<Page<EventResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("GET /api/v1/events - Request received to retrieve all events - page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<EventResponse> response = eventServicePort.getAll(pageable);
        log.info("GET /api/v1/events - Retrieved {} events (page {} of {})", response.getNumberOfElements(), response.getNumber(), response.getTotalPages());
        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing event entry with the provided data.
     *
     * @param id      the unique business identifier (path variable)
     * @param request the validated update request body
     * @return the updated event with HTTP 200 status
     */
    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateEventRequest request) {
        log.info("PUT /api/v1/events/{} - Request received to update event", id);
        EventResponse response = eventServicePort.update(id, request);
        log.info("PUT /api/v1/events/{} - Event updated successfully", id);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft-deletes an event entry by its unique ID.
     * <p>
     * The record is not physically removed from the database; it is
     * marked as deleted and excluded from future active queries.
     * </p>
     *
     * @param id the unique business identifier (path variable)
     * @return HTTP 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        log.info("DELETE /api/v1/events/{} - Request received to soft-delete event", id);
        eventServicePort.delete(id);
        log.info("DELETE /api/v1/events/{} - Event soft-deleted successfully", id);
        return ResponseEntity.noContent().build();
    }

}

