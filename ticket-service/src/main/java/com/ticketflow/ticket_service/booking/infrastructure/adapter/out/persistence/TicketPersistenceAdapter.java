package com.ticketflow.ticket_service.booking.infrastructure.adapter.out.persistence;

import com.ticketflow.ticket_service.booking.domain.model.Ticket;
import com.ticketflow.ticket_service.booking.domain.port.out.ITicketPersistencePort;
import com.ticketflow.ticket_service.booking.infrastructure.adapter.out.persistence.mapper.ITicketPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Persistence adapter implementing the {@link ITicketPersistencePort} outbound port.
 * <p>
 * This adapter bridges the domain layer with the JPA infrastructure,
 * converting between domain objects and JPA entities using
 * {@link ITicketPersistenceMapper}.
 * </p>
 *
 * @author TicketFlow Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketPersistenceAdapter implements ITicketPersistencePort {

    private final ITicketJpaRepository ticketJpaRepository;
    private final ITicketPersistenceMapper ticketPersistenceMapper;

    /**
     * {@inheritDoc}
     * <p>
     * Converts the domain object to a JPA entity, persists it via the JPA repository,
     * and converts the saved entity back to a domain object.
     * </p>
     */
    @Override
    public Ticket save(Ticket ticket) {
        log.debug("Saving ticket entity with id: {}", ticket.getId());
        TicketEntity entity = ticketPersistenceMapper.toEntity(ticket);
        TicketEntity savedEntity = ticketJpaRepository.save(entity);
        savedEntity.setUpdatedAt(null);
        log.debug("Ticket entity saved successfully with id: {}", savedEntity.getId());
        return ticketPersistenceMapper.toDomain(savedEntity);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Queries the JPA repository for an active entity and maps it to a domain object
     * if present. Returns {@link Optional#empty()} if the entity does not exist or
     * has been soft-deleted.
     * </p>
     */
    @Override
    public Optional<Ticket> findByIdAndDeletedFalse(String id) {
        log.debug("Finding active ticket entity with id: {}", id);
        Optional<Ticket> result = ticketJpaRepository.findByIdAndDeletedFalse(id)
                .map(ticketPersistenceMapper::toDomain);
        log.debug("Ticket entity with id '{}' {}", id, result.isPresent() ? "found" : "not found");
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Builds a {@link Specification} combining the mandatory soft-delete filter with
     * optional eventId, userId, and status predicates, then delegates to the JPA
     * repository's specification executor.
     * </p>
     */
    @Override
    public Page<Ticket> findAllByFilters(String eventId, String userId, String status, Pageable pageable) {
        log.debug("Finding tickets with filters - eventId: {}, userId: {}, status: {}, page: {}, size: {}",
                eventId, userId, status, pageable.getPageNumber(), pageable.getPageSize());

        Specification<TicketEntity> spec = TicketSpecification.notDeleted();
        if (StringUtils.hasText(eventId)) {
            spec = spec.and(TicketSpecification.eventIdEquals(eventId));
        }
        if (StringUtils.hasText(userId)) {
            spec = spec.and(TicketSpecification.userIdEquals(userId));
        }
        if (StringUtils.hasText(status)) {
            spec = spec.and(TicketSpecification.statusEquals(status));
        }

        Page<Ticket> result = ticketJpaRepository.findAll(spec, pageable)
                .map(ticketPersistenceMapper::toDomain);
        log.debug("Found {} ticket entities out of {} total", result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Converts the updated domain object to a JPA entity and calls
     * {@link ITicketJpaRepository#save(Object)}, which performs a merge
     * since the entity already has a persisted ID.
     * </p>
     */
    @Override
    public Ticket update(Ticket ticket) {
        log.debug("Updating ticket entity with id: {}", ticket.getId());
        TicketEntity entity = ticketPersistenceMapper.toEntity(ticket);
        TicketEntity updatedEntity = ticketJpaRepository.save(entity);
        log.debug("Ticket entity updated successfully with id: {}", updatedEntity.getId());
        return ticketPersistenceMapper.toDomain(updatedEntity);
    }
}
