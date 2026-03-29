package com.ticketflow.event_service.catalog.infrastructure.adapter.out.persistence;

import com.ticketflow.event_service.catalog.domain.model.Event;
import com.ticketflow.event_service.catalog.domain.port.out.IEventPersistencePort;
import com.ticketflow.event_service.catalog.infrastructure.adapter.out.persistence.mapper.IEventPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * Persistence adapter implementing the {@link IEventPersistencePort} outbound port.
 * <p>
 * This adapter bridges the domain layer with the JPA infrastructure,
 * converting between domain objects and JPA entities using
 * {@link IEventPersistenceMapper}.
 * </p>
 *
 * @author TicketFlow Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPersistenceAdapter implements IEventPersistencePort {

    private final IEventJpaRepository eventJpaRepository;
    private final IEventPersistenceMapper eventPersistenceMapper;

    /**
     * {@inheritDoc}
     * <p>
     * Converts the domain object to a JPA entity, persists it via the JPA repository,
     * and converts the saved entity back to a domain object.
     * </p>
     */
    @Override
    public Event save(Event event) {
        log.debug("Saving event entity with id: {}", event.getId());
        EventEntity entity = eventPersistenceMapper.toEntity(event);
        EventEntity savedEntity = eventJpaRepository.save(entity);
        log.debug("Event entity saved successfully with id: {}", savedEntity.getId());
        return eventPersistenceMapper.toDomain(savedEntity);
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
    public Optional<Event> findByIdAndDeletedFalse(String id) {
        log.debug("Finding active event entity with id: {}", id);
        Optional<Event> result = eventJpaRepository.findByIdAndDeletedFalse(id)
                .map(eventPersistenceMapper::toDomain);
        log.debug("Event entity with id '{}' {}", id, result.isPresent() ? "found" : "not found");
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Fetches a page of active entities from the JPA repository and maps each
     * one to a domain object. Pagination and sorting are handled by the provided
     * {@link Pageable} parameters.
     * </p>
     */
    @Override
    public Page<Event> findAllByDeletedFalse(Pageable pageable) {
        log.debug("Finding all active event entities - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<Event> result = eventJpaRepository.findAllByDeletedFalse(pageable)
                .map(eventPersistenceMapper::toDomain);
        log.debug("Found {} active event entities out of {} total", result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Builds a {@link Specification} combining mandatory soft-delete filter with
     * optional title and location LIKE predicates, then delegates to the JPA
     * repository's specification executor.
     * </p>
     */
    @Override
    public Page<Event> findAllByFilters(String title, String location, Pageable pageable) {
        log.debug("Finding events with filters - title: {}, location: {}, page: {}, size: {}",
                title, location, pageable.getPageNumber(), pageable.getPageSize());

        Specification<EventEntity> spec = EventSpecification.notDeleted();
        if (StringUtils.hasText(title)) {
            spec = spec.and(EventSpecification.titleContains(title));
        }
        if (StringUtils.hasText(location)) {
            spec = spec.and(EventSpecification.locationContains(location));
        }

        Page<Event> result = eventJpaRepository.findAll(spec, pageable)
                .map(eventPersistenceMapper::toDomain);
        log.debug("Found {} event entities out of {} total", result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Converts the updated domain object to a JPA entity and calls
     * {@link IEventJpaRepository#save(Object)}, which performs a merge
     * since the entity already has a persisted ID.
     * </p>
     */
    @Override
    public Event update(Event event) {
        log.debug("Updating event entity with id: {}", event.getId());
        EventEntity entity = eventPersistenceMapper.toEntity(event);
        EventEntity updatedEntity = eventJpaRepository.save(entity);
        log.debug("Event entity updated successfully with id: {}", updatedEntity.getId());
        return eventPersistenceMapper.toDomain(updatedEntity);
    }

    @Override
    public Page<Event> findAllByCreatorIdAndDeletedFalse(String creatorId, Pageable pageable) {
        log.debug("Finding events by creatorId: {}", creatorId);
        return eventJpaRepository.findAllByCreatorIdAndDeletedFalse(creatorId, pageable)
                .map(eventPersistenceMapper::toDomain);
    }

    @Override
    public List<String> findIdsByCreatorIdAndDeletedFalse(String creatorId) {
        log.debug("Finding event IDs by creatorId: {}", creatorId);
        return eventJpaRepository.findIdsByCreatorIdAndDeletedFalse(creatorId);
    }
}

