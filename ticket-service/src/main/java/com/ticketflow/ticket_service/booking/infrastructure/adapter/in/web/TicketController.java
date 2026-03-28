package com.ticketflow.ticket_service.booking.infrastructure.adapter.in.web;

import com.ticketflow.ticket_service.booking.application.dto.request.CreateTicketRequest;
import com.ticketflow.ticket_service.booking.application.dto.response.TicketResponse;
import com.ticketflow.ticket_service.booking.domain.port.in.ITicketService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing CRUD and cancellation endpoints for ticket management.
 * <p>
 * This is an inbound adapter in the hexagonal architecture. It receives
 * HTTP requests, delegates to the {@link ITicketService}, and returns
 * appropriate HTTP responses.
 * </p>
 * <p>
 * Base path: {@code /api/v1/tickets}
 * </p>
 *
 * @author TicketFlow Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Tag(name = "Tickets", description = "Ticket booking management endpoints")
public class TicketController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ITicketService ticketServicePort;

    @Operation(summary = "Purchase a new ticket")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ticket purchased successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body")
    })
    @PostMapping
    public ResponseEntity<TicketResponse> create(
            @Valid @RequestBody CreateTicketRequest request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Email") String userEmail) {
        log.info("POST /api/v1/tickets - Request received to purchase ticket");
        TicketResponse response = ticketServicePort.create(request, userId, userEmail);
        log.info("POST /api/v1/tickets - Ticket created successfully with id: {}", response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get a ticket by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket found"),
            @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getById(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId) {
        log.info("GET /api/v1/tickets/{} - Request received to retrieve ticket", id);
        TicketResponse response = ticketServicePort.getById(id, userId);
        log.info("GET /api/v1/tickets/{} - Ticket retrieved successfully", id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List tickets with optional filters and pagination")
    @ApiResponse(responseCode = "200", description = "Paginated list of tickets")
    @GetMapping
    public ResponseEntity<Page<TicketResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String eventId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestHeader("X-User-Id") String userId) {
        size = Math.min(size, MAX_PAGE_SIZE);
        log.info("GET /api/v1/tickets - Request received - page: {}, size: {}, eventId: {}, status: {}, sortBy: {}, sortDir: {}",
                page, size, eventId, status, sortBy, sortDir);
        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<TicketResponse> response = ticketServicePort.getAll(eventId, status, pageable, userId);
        log.info("GET /api/v1/tickets - Retrieved {} tickets (page {} of {})",
                response.getNumberOfElements(), response.getNumber(), response.getTotalPages());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cancel an active ticket")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket cancelled successfully"),
            @ApiResponse(responseCode = "404", description = "Ticket not found"),
            @ApiResponse(responseCode = "409", description = "Ticket already cancelled")
    })
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<TicketResponse> cancel(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Email") String userEmail) {
        log.info("PATCH /api/v1/tickets/{}/cancel - Request received to cancel ticket", id);
        TicketResponse response = ticketServicePort.cancel(id, userId, userEmail);
        log.info("PATCH /api/v1/tickets/{}/cancel - Ticket cancelled successfully", id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Soft-delete a ticket by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ticket deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId) {
        log.info("DELETE /api/v1/tickets/{} - Request received to soft-delete ticket", id);
        ticketServicePort.delete(id, userId);
        log.info("DELETE /api/v1/tickets/{} - Ticket soft-deleted successfully", id);
        return ResponseEntity.noContent().build();
    }
}
