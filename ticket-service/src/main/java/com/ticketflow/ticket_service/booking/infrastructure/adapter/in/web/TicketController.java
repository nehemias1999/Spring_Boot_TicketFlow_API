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
 * Base path: {@code /api/v1/tickets}
 * </p>
 * Role enforcement:
 * <ul>
 *   <li>POST: USER, ADMIN</li>
 *   <li>GET/{id}: USER/SELLER own ticket; MODERATOR/ADMIN any</li>
 *   <li>GET: USER own; SELLER own-events; MODERATOR/ADMIN all</li>
 *   <li>PATCH cancel: USER (own), ADMIN</li>
 *   <li>DELETE: USER (own), ADMIN</li>
 * </ul>
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

    @Operation(summary = "Purchase a new ticket (USER, ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ticket purchased successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "403", description = "Insufficient role")
    })
    @PostMapping
    public ResponseEntity<TicketResponse> create(
            @Valid @RequestBody CreateTicketRequest request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Role") String userRole) {
        log.info("POST /api/v1/tickets - userId: {}, role: {}", userId, userRole);
        TicketResponse response = ticketServicePort.create(request, userId, userEmail, userRole);
        log.info("POST /api/v1/tickets - Ticket created with id: {}", response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get a ticket by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket found"),
            @ApiResponse(responseCode = "403", description = "Not the owner"),
            @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getById(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {
        log.info("GET /api/v1/tickets/{} - userId: {}, role: {}", id, userId, userRole);
        TicketResponse response = ticketServicePort.getById(id, userId, userRole);
        log.info("GET /api/v1/tickets/{} - Ticket retrieved successfully", id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List tickets (role-dependent scope)")
    @ApiResponse(responseCode = "200", description = "Paginated list of tickets")
    @GetMapping
    public ResponseEntity<Page<TicketResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String eventId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {
        size = Math.min(size, MAX_PAGE_SIZE);
        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        log.info("GET /api/v1/tickets - role: {}, userId: {}", userRole, userId);
        Page<TicketResponse> response = ticketServicePort.getAll(eventId, status, pageable, userId, userRole);
        log.info("GET /api/v1/tickets - Retrieved {} tickets", response.getNumberOfElements());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cancel an active ticket (USER: own; ADMIN: any)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket cancelled successfully"),
            @ApiResponse(responseCode = "403", description = "Insufficient role or not the owner"),
            @ApiResponse(responseCode = "404", description = "Ticket not found"),
            @ApiResponse(responseCode = "409", description = "Ticket already cancelled")
    })
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<TicketResponse> cancel(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Role") String userRole) {
        log.info("PATCH /api/v1/tickets/{}/cancel - userId: {}, role: {}", id, userId, userRole);
        TicketResponse response = ticketServicePort.cancel(id, userId, userEmail, userRole);
        log.info("PATCH /api/v1/tickets/{}/cancel - Ticket cancelled successfully", id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Soft-delete a ticket (USER: own; ADMIN: any)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ticket deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Insufficient role or not the owner"),
            @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {
        log.info("DELETE /api/v1/tickets/{} - userId: {}, role: {}", id, userId, userRole);
        ticketServicePort.delete(id, userId, userRole);
        log.info("DELETE /api/v1/tickets/{} - Ticket soft-deleted successfully", id);
        return ResponseEntity.noContent().build();
    }
}
