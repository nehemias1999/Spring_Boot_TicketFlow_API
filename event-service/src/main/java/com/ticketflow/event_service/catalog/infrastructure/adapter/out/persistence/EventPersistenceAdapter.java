package com.ticketflow.event_service.catalog.infrastructure.adapter.out.persistence;

import com.ticketflow.event_service.catalog.domain.model.Event;
import com.ticketflow.event_service.catalog.domain.port.out.IEventPersistencePort;
import com.ticketflow.event_service.catalog.infrastructure.adapter.out.persistence.mapper.IEventPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

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

    @Override
    public Event save(Event event) {
        log.debug("Saving event entity with id: {}", event.getId());
        EventEntity entity = eventPersistenceMapper.toEntity(event);
        EventEntity savedEntity = eventJpaRepository.save(entity);
        log.debug("Event entity saved successfully with id: {}", savedEntity.getId());
        return eventPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Event> findByIdAndDeletedFalse(String id) {
        log.debug("Finding active event entity with id: {}", id);
        Optional<Event> result = eventJpaRepository.findByIdAndDeletedFalse(id)
                .map(eventPersistenceMapper::toDomain);
        log.debug("Event entity with id '{}' {}", id, result.isPresent() ? "found" : "not found");
        return result;
    }

    @Override
    public Page<Event> findAllByDeletedFalse(Pageable pageable) {
        log.debug("Finding all active event entities - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<Event> result = eventJpaRepository.findAllByDeletedFalse(pageable)
                .map(eventPersistenceMapper::toDomain);
        log.debug("Found {} active event entities out of {} total", result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    @Override
    public boolean existsByIdAndDeletedFalse(String id) {
        log.debug("Checking existence of active event entity with id: {}", id);
        boolean exists = eventJpaRepository.existsByIdAndDeletedFalse(id);
        log.debug("Active event entity with id '{}' exists: {}", id, exists);
        return exists;
    }

    @Override
    public Event update(Event event) {
        log.debug("Updating event entity with id: {}", event.getId());
        EventEntity entity = eventPersistenceMapper.toEntity(event);
        EventEntity updatedEntity = eventJpaRepository.save(entity);
        log.debug("Event entity updated successfully with id: {}", updatedEntity.getId());
        return eventPersistenceMapper.toDomain(updatedEntity);
    }

}

