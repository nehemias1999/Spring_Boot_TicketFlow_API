package com.ticketflow.ticket_service.booking.application.service;

import com.ticketflow.ticket_service.booking.application.dto.request.CreateTicketRequest;
import com.ticketflow.ticket_service.booking.application.dto.request.UpdateTicketRequest;
import com.ticketflow.ticket_service.booking.application.dto.response.TicketResponse;
import com.ticketflow.ticket_service.booking.application.mapper.ITicketApplicationMapper;
import com.ticketflow.ticket_service.booking.domain.exception.TicketAlreadyCancelledException;
import com.ticketflow.ticket_service.booking.domain.exception.TicketNotFoundException;
import com.ticketflow.ticket_service.booking.domain.model.Ticket;
import com.ticketflow.ticket_service.booking.domain.model.TicketStatus;
import com.ticketflow.ticket_service.booking.domain.port.in.ITicketService;
import com.ticketflow.ticket_service.booking.domain.port.out.ITicketEventPublisher;
import com.ticketflow.ticket_service.booking.domain.port.out.ITicketPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Application service implementing the {@link ITicketService} inbound port.
 * <p>
 * Contains all business logic for ticket CRUD operations including
 * cancellation guards, soft-delete handling, and delegation to the
 * persistence outbound port.
 * </p>
 *
 * @author TicketFlow Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TicketService implements ITicketService {

    private final ITicketPersistencePort ticketPersistencePort;
    private final ITicketApplicationMapper ticketApplicationMapper;
    private final ITicketEventPublisher ticketEventPublisher;

    /**
     * {@inheritDoc}
     * <p>
     * Generates a UUID as the ticket ID server-side.
     * Sets {@code purchaseDate} to now and {@code status} to {@link TicketStatus#CONFIRMED}.
     * </p>
     */
    @Override
    public TicketResponse create(CreateTicketRequest request) {
        String id = UUID.randomUUID().toString();
        log.info("Creating ticket with generated id: {}", id);

        Ticket ticket = ticketApplicationMapper.toDomain(request);
        ticket.setId(id);
        ticket.setPurchaseDate(LocalDateTime.now());
        ticket.setStatus(TicketStatus.CONFIRMED);

        Ticket savedTicket = ticketPersistencePort.save(ticket);

        ticketEventPublisher.publishTicketPurchased(savedTicket.getId(), savedTicket.getUserId());

        log.info("Ticket created successfully with id: {}", savedTicket.getId());
        return ticketApplicationMapper.toResponse(savedTicket);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Throws {@link TicketNotFoundException} if no active ticket with the given ID exists.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    public TicketResponse getById(String id) {
        log.info("Retrieving ticket with id: {}", id);

        Ticket ticket = ticketPersistencePort.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Ticket retrieval failed - ticket with id '{}' not found", id);
                    return new TicketNotFoundException(id);
                });

        log.info("Ticket retrieved successfully with id: {}", id);
        return ticketApplicationMapper.toResponse(ticket);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Delegates pagination and filtering directly to the persistence port and maps
     * each domain object to a response DTO.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    public Page<TicketResponse> getAll(String eventId, String userId, String status, Pageable pageable) {
        log.info("Retrieving all tickets - eventId: {}, userId: {}, status: {}, page: {}, size: {}",
                eventId, userId, status, pageable.getPageNumber(), pageable.getPageSize());

        Page<TicketResponse> result = ticketPersistencePort.findAllByFilters(eventId, userId, status, pageable)
                .map(ticketApplicationMapper::toResponse);

        log.info("Retrieved {} tickets out of {} total", result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Loads the existing ticket, applies the userId update via the application mapper,
     * and persists the changes. Throws {@link TicketNotFoundException} if the ticket
     * does not exist or has been soft-deleted.
     * </p>
     */
    @Override
    public TicketResponse update(String id, UpdateTicketRequest request) {
        log.info("Updating ticket with id: {}", id);

        Ticket existingTicket = ticketPersistencePort.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Ticket update failed - ticket with id '{}' not found", id);
                    return new TicketNotFoundException(id);
                });

        ticketApplicationMapper.updateDomainFromRequest(request, existingTicket);
        Ticket savedTicket = ticketPersistencePort.update(existingTicket);

        log.info("Ticket updated successfully with id: {}", savedTicket.getId());
        return ticketApplicationMapper.toResponse(savedTicket);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Throws {@link TicketNotFoundException} if the ticket does not exist.
     * Throws {@link TicketAlreadyCancelledException} if the ticket is already cancelled.
     * </p>
     */
    @Override
    public TicketResponse cancel(String id) {
        log.info("Cancelling ticket with id: {}", id);

        Ticket ticket = ticketPersistencePort.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Ticket cancellation failed - ticket with id '{}' not found", id);
                    return new TicketNotFoundException(id);
                });

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            log.warn("Ticket cancellation failed - ticket with id '{}' is already cancelled", id);
            throw new TicketAlreadyCancelledException(id);
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        Ticket savedTicket = ticketPersistencePort.update(ticket);

        log.info("Ticket cancelled successfully with id: {}", id);
        return ticketApplicationMapper.toResponse(savedTicket);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Sets the {@code deleted} flag to {@code true} and persists the change.
     * The record remains in the database and is excluded from active queries.
     * Throws {@link TicketNotFoundException} if the ticket does not exist.
     * </p>
     */
    @Override
    public void delete(String id) {
        log.info("Soft-deleting ticket with id: {}", id);

        Ticket ticket = ticketPersistencePort.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Ticket soft-delete failed - ticket with id '{}' not found", id);
                    return new TicketNotFoundException(id);
                });

        ticket.setDeleted(true);
        ticketPersistencePort.update(ticket);

        log.info("Ticket soft-deleted successfully with id: {}", id);
    }
}
