package com.ticketflow.ticket_service.booking.application.service;

import com.ticketflow.ticket_service.booking.application.dto.external.EventDto;
import com.ticketflow.ticket_service.booking.application.dto.request.CreateTicketRequest;
import com.ticketflow.ticket_service.booking.application.dto.response.TicketResponse;
import com.ticketflow.ticket_service.booking.application.mapper.ITicketApplicationMapper;
import com.ticketflow.ticket_service.booking.domain.exception.AccessDeniedException;
import com.ticketflow.ticket_service.booking.domain.exception.TicketAlreadyCancelledException;
import com.ticketflow.ticket_service.booking.domain.exception.TicketNotFoundException;
import com.ticketflow.ticket_service.booking.domain.exception.TicketOwnershipException;
import com.ticketflow.ticket_service.booking.domain.model.Ticket;
import com.ticketflow.ticket_service.booking.domain.model.TicketStatus;
import com.ticketflow.ticket_service.booking.domain.port.in.ITicketService;
import com.ticketflow.ticket_service.booking.domain.port.out.IEventServicePort;
import com.ticketflow.ticket_service.booking.domain.port.out.ITicketEventPublisher;
import com.ticketflow.ticket_service.booking.domain.port.out.ITicketPersistencePort;
import com.ticketflow.ticket_service.shared.infrastructure.security.RoleValidator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Application service implementing the {@link ITicketService} inbound port.
 *
 * @author TicketFlow Team
 */
@Slf4j
@Service
@Transactional
public class TicketService implements ITicketService {

    private final ITicketPersistencePort ticketPersistencePort;
    private final ITicketApplicationMapper ticketApplicationMapper;
    private final ITicketEventPublisher ticketEventPublisher;
    private final IEventServicePort eventServicePort;
    private final Counter ticketsPurchasedCounter;
    private final Counter ticketsCancelledCounter;

    public TicketService(ITicketPersistencePort ticketPersistencePort,
                         ITicketApplicationMapper ticketApplicationMapper,
                         ITicketEventPublisher ticketEventPublisher,
                         IEventServicePort eventServicePort,
                         MeterRegistry meterRegistry) {
        this.ticketPersistencePort = ticketPersistencePort;
        this.ticketApplicationMapper = ticketApplicationMapper;
        this.ticketEventPublisher = ticketEventPublisher;
        this.eventServicePort = eventServicePort;
        this.ticketsPurchasedCounter = Counter.builder("tickets.purchased")
                .description("Total number of tickets purchased")
                .register(meterRegistry);
        this.ticketsCancelledCounter = Counter.builder("tickets.cancelled")
                .description("Total number of tickets cancelled")
                .register(meterRegistry);
    }

    @Override
    public TicketResponse create(CreateTicketRequest request, String authenticatedUserId,
                                 String userEmail, String userRole) {
        RoleValidator.requireAnyRole(userRole, "USER", "ADMIN");

        // Improvement 2: Verify event exists and has available capacity
        EventDto event = eventServicePort.getEventById(request.eventId());
        eventServicePort.decrementAvailableTickets(request.eventId());

        String id = UUID.randomUUID().toString();
        log.info("Creating ticket '{}' for userId: {}", id, authenticatedUserId);

        Ticket ticket = ticketApplicationMapper.toDomain(request);
        ticket.setId(id);
        ticket.setUserId(authenticatedUserId);
        ticket.setPurchaseDate(LocalDateTime.now());
        ticket.setStatus(TicketStatus.CONFIRMED);
        ticket.setPrice(event.basePrice()); // Improvement 3: record price at purchase time

        Ticket savedTicket = ticketPersistencePort.save(ticket);
        ticketsPurchasedCounter.increment();

        // Publish after commit so a messaging failure never rolls back the saved ticket
        final String ticketId    = savedTicket.getId();
        final String ticketOwner = savedTicket.getUserId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    ticketEventPublisher.publishTicketPurchased(ticketId, ticketOwner, userEmail);
                } catch (Exception e) {
                    log.warn("Failed to publish TicketPurchasedEvent for ticket '{}': {}", ticketId, e.getMessage());
                }
            }
        });

        log.info("Ticket created successfully with id: {}", savedTicket.getId());
        return ticketApplicationMapper.toResponse(savedTicket);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getById(String id, String authenticatedUserId, String userRole) {
        log.info("Retrieving ticket '{}'", id);

        Ticket ticket = ticketPersistencePort.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Ticket '{}' not found", id);
                    return new TicketNotFoundException(id);
                });

        boolean isPrivileged = "ADMIN".equalsIgnoreCase(userRole)
                || "MODERATOR".equalsIgnoreCase(userRole);

        if (!isPrivileged && !ticket.getUserId().equals(authenticatedUserId)) {
            log.warn("userId '{}' does not own ticket '{}'", authenticatedUserId, id);
            throw new TicketOwnershipException(id);
        }

        return ticketApplicationMapper.toResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketResponse> getAll(String eventId, String status, Pageable pageable,
                                       String authenticatedUserId, String userRole) {
        log.info("Listing tickets - role: {}, userId: {}, eventId: {}, status: {}",
                userRole, authenticatedUserId, eventId, status);

        if ("ADMIN".equalsIgnoreCase(userRole) || "MODERATOR".equalsIgnoreCase(userRole)) {
            return ticketPersistencePort
                    .findAllByFilters(eventId, null, status, pageable)
                    .map(ticketApplicationMapper::toResponse);
        }

        if ("SELLER".equalsIgnoreCase(userRole)) {
            List<String> sellerEventIds = eventServicePort.getSellerEventIds(authenticatedUserId);
            log.debug("SELLER '{}' owns {} events", authenticatedUserId, sellerEventIds.size());
            return ticketPersistencePort
                    .findByEventIdsIn(sellerEventIds, pageable)
                    .map(ticketApplicationMapper::toResponse);
        }

        // USER: own tickets only
        return ticketPersistencePort
                .findAllByFilters(eventId, authenticatedUserId, status, pageable)
                .map(ticketApplicationMapper::toResponse);
    }

    @Override
    public TicketResponse cancel(String id, String authenticatedUserId, String userEmail, String userRole) {
        RoleValidator.requireAnyRole(userRole, "USER", "ADMIN");
        log.info("Cancelling ticket '{}'", id);

        Ticket ticket = ticketPersistencePort.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Ticket '{}' not found for cancellation", id);
                    return new TicketNotFoundException(id);
                });

        if (!"ADMIN".equalsIgnoreCase(userRole) && !ticket.getUserId().equals(authenticatedUserId)) {
            log.warn("userId '{}' does not own ticket '{}'", authenticatedUserId, id);
            throw new TicketOwnershipException(id);
        }

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            log.warn("Ticket '{}' is already cancelled", id);
            throw new TicketAlreadyCancelledException(id);
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        Ticket saved = ticketPersistencePort.update(ticket);
        ticketsCancelledCounter.increment();

        // Restore capacity in event-service
        try {
            eventServicePort.incrementAvailableTickets(ticket.getEventId());
        } catch (Exception e) {
            log.warn("Failed to increment available tickets for event '{}': {}", ticket.getEventId(), e.getMessage());
        }

        // Publish after commit so a messaging failure never rolls back the cancellation
        final String cancelledId    = saved.getId();
        final String cancelledOwner = saved.getUserId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    ticketEventPublisher.publishTicketCancelled(cancelledId, cancelledOwner, userEmail);
                } catch (Exception e) {
                    log.warn("Failed to publish TicketCancelledEvent for ticket '{}': {}", cancelledId, e.getMessage());
                }
            }
        });

        log.info("Ticket '{}' cancelled successfully", id);
        return ticketApplicationMapper.toResponse(saved);
    }

    @Override
    public void delete(String id, String authenticatedUserId, String userRole) {
        RoleValidator.requireAnyRole(userRole, "USER", "ADMIN");
        log.info("Soft-deleting ticket '{}'", id);

        Ticket ticket = ticketPersistencePort.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Ticket '{}' not found for deletion", id);
                    return new TicketNotFoundException(id);
                });

        if (!"ADMIN".equalsIgnoreCase(userRole) && !ticket.getUserId().equals(authenticatedUserId)) {
            log.warn("userId '{}' does not own ticket '{}'", authenticatedUserId, id);
            throw new TicketOwnershipException(id);
        }

        ticket.setDeleted(true);
        ticketPersistencePort.update(ticket);
        log.info("Ticket '{}' soft-deleted successfully", id);
    }
}
