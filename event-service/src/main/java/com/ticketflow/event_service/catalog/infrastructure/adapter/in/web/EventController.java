package com.ticketflow.event_service.catalog.infrastructure.adapter.in.web;

import com.ticketflow.event_service.catalog.application.dto.response.EventResponse;
import com.ticketflow.event_service.catalog.application.dto.request.CreateEventRequest;
import com.ticketflow.event_service.catalog.application.dto.request.UpdateEventRequest;
import com.ticketflow.event_service.catalog.domain.port.in.IEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
@Tag(name = "Events", description = "Event management endpoints")
public class EventController {

    private static final int MAX_PAGE_SIZE = 100;

    private final IEventService eventServicePort;

    @Operation(summary = "Create a new event")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Event created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body")
    })
    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        log.info("POST /api/v1/events - Request received to create event");
        EventResponse response = eventServicePort.create(request);
        log.info("POST /api/v1/events - Event created successfully with id: {}", response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get an event by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event found"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getById(@PathVariable String id) {
        log.info("GET /api/v1/events/{} - Request received to retrieve event", id);
        EventResponse response = eventServicePort.getById(id);
        log.info("GET /api/v1/events/{} - Event retrieved successfully", id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List events with optional filters and pagination")
    @ApiResponse(responseCode = "200", description = "Paginated list of events")
    @GetMapping
    public ResponseEntity<Page<EventResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        size = Math.min(size, MAX_PAGE_SIZE);
        log.info("GET /api/v1/events - Request received - page: {}, size: {}, title: {}, location: {}, sortBy: {}, sortDir: {}",
                page, size, title, location, sortBy, sortDir);
        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<EventResponse> response = eventServicePort.getAll(title, location, pageable);
        log.info("GET /api/v1/events - Retrieved {} events (page {} of {})", response.getNumberOfElements(), response.getNumber(), response.getTotalPages());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update an existing event")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateEventRequest request) {
        log.info("PUT /api/v1/events/{} - Request received to update event", id);
        EventResponse response = eventServicePort.update(id, request);
        log.info("PUT /api/v1/events/{} - Event updated successfully", id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Soft-delete an event by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Event deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        log.info("DELETE /api/v1/events/{} - Request received to soft-delete event", id);
        eventServicePort.delete(id);
        log.info("DELETE /api/v1/events/{} - Event soft-deleted successfully", id);
        return ResponseEntity.noContent().build();
    }

}
