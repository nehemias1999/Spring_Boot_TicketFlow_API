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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller exposing CRUD endpoints for event management.
 * <p>
 * Base path: {@code /api/v1/events}
 * </p>
 *
 * @author TicketFlow Team
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Event management endpoints")
public class EventController {

    private final IEventService eventServicePort;

    @Operation(summary = "Create a new event (SELLER, ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Event created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "403", description = "Insufficient role")
    })
    @PostMapping
    public ResponseEntity<EventResponse> create(
            @Valid @RequestBody CreateEventRequest request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {
        log.info("POST /api/v1/events - userId: {}, role: {}", userId, userRole);
        EventResponse response = eventServicePort.create(request, userId, userRole);
        log.info("POST /api/v1/events - Event created with id: {}", response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get an event by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event found"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getById(@PathVariable String id) {
        log.info("GET /api/v1/events/{}", id);
        return ResponseEntity.ok(eventServicePort.getById(id));
    }

    @Operation(summary = "List events with optional filters and pagination")
    @ApiResponse(responseCode = "200", description = "Paginated list of events")
    @GetMapping
    public ResponseEntity<Page<EventResponse>> getAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<EventResponse> response = eventServicePort.getAll(title, location, pageable);
        log.info("GET /api/v1/events - Retrieved {} events", response.getNumberOfElements());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update an existing event (SELLER: own; ADMIN: any)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "403", description = "Insufficient role or not the owner"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateEventRequest request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {
        log.info("PUT /api/v1/events/{} - userId: {}, role: {}", id, userId, userRole);
        EventResponse response = eventServicePort.update(id, request, userId, userRole);
        log.info("PUT /api/v1/events/{} - Event updated successfully", id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Soft-delete an event (SELLER: own; ADMIN: any)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Event deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Insufficient role or not the owner"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {
        log.info("DELETE /api/v1/events/{} - userId: {}, role: {}", id, userId, userRole);
        eventServicePort.delete(id, userId, userRole);
        log.info("DELETE /api/v1/events/{} - Event soft-deleted successfully", id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List SELLER's own events")
    @ApiResponse(responseCode = "200", description = "Paginated list of the SELLER's own events")
    @GetMapping("/my")
    public ResponseEntity<Page<EventResponse>> getMyEvents(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        com.ticketflow.event_service.shared.infrastructure.security.RoleValidator
                .requireAnyRole(userRole, "SELLER", "ADMIN");
        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        log.info("GET /api/v1/events/my - userId: {}", userId);
        return ResponseEntity.ok(eventServicePort.getMyEvents(userId, pageable));
    }

    @Operation(summary = "Return IDs of the caller's events (internal use by ticket-service)")
    @ApiResponse(responseCode = "200", description = "List of event IDs")
    @GetMapping("/my-event-ids")
    public ResponseEntity<List<String>> getMyEventIds(
            @RequestHeader("X-User-Id") String userId) {
        log.debug("GET /api/v1/events/my-event-ids - userId: {}", userId);
        return ResponseEntity.ok(eventServicePort.getMyEventIds(userId));
    }

    @Operation(summary = "Decrement available tickets for an event (internal use by ticket-service)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Available tickets decremented"),
            @ApiResponse(responseCode = "404", description = "Event not found"),
            @ApiResponse(responseCode = "409", description = "Event has no available tickets")
    })
    @PatchMapping("/{id}/decrement-tickets")
    public ResponseEntity<EventResponse> decrementTickets(@PathVariable String id) {
        log.debug("PATCH /api/v1/events/{}/decrement-tickets", id);
        return ResponseEntity.ok(eventServicePort.decrementAvailableTickets(id));
    }

    @Operation(summary = "Increment available tickets for an event (internal use by ticket-service)")
    @ApiResponse(responseCode = "200", description = "Available tickets incremented")
    @PatchMapping("/{id}/increment-tickets")
    public ResponseEntity<EventResponse> incrementTickets(@PathVariable String id) {
        log.debug("PATCH /api/v1/events/{}/increment-tickets", id);
        return ResponseEntity.ok(eventServicePort.incrementAvailableTickets(id));
    }
}
