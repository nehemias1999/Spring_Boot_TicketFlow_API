package com.ticketflow.ticket_service.booking.application.service;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@RequiredArgsConstructor
@Transactional
public class TicketService implements ITicketService {

    private final ITicketPersistencePort ticketPersistencePort;
    private final ITicketApplicationMapper ticketApplicationMapper;
    private final ITicketEventPublisher ticketEventPublisher;
    private final IEventServicePort eventServicePort;

    @Override
    public TicketResponse create(CreateTicketRequest request, String authenticatedUserId,
                                 String userEmail, String userRole) {
        RoleValidator.requireAnyRole(userRole, "USER", "ADMIN");

        String id = UUID.randomUUID().toString();
        log.info("Creating ticket '{}' for userId: {}", id, authenticatedUserId);

        Ticket ticket = ticketApplicationMapper.toDomain(request);
        ticket.setId(id);
        ticket.setUserId(authenticatedUserId);
        ticket.setPurchaseDate(LocalDateTime.now());
        ticket.setStatus(TicketStatus.CONFIRMED);

        Ticket savedTicket = ticketPersistencePort.save(ticket);
        ticketEventPublisher.publishTicketPurchased(savedTicket.getId(), savedTicket.getUserId(), userEmail);

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
        ticketEventPublisher.publishTicketCancelled(saved.getId(), saved.getUserId(), userEmail);

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
