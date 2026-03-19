package com.ticketflow.event_service.catalog.application.service;

import com.ticketflow.event_service.catalog.application.dto.response.EventResponse;
import com.ticketflow.event_service.catalog.application.dto.request.CreateEventRequest;
import com.ticketflow.event_service.catalog.application.dto.request.UpdateEventRequest;
import com.ticketflow.event_service.catalog.application.mapper.IEventApplicationMapper;
import com.ticketflow.event_service.catalog.domain.exception.EventNotFoundException;
import com.ticketflow.event_service.catalog.domain.model.Event;
import com.ticketflow.event_service.catalog.domain.port.in.IEventService;
import com.ticketflow.event_service.catalog.domain.port.out.IEventPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service implementing the {@link IEventService} inbound port.
 * <p>
 * Contains all business logic for event CRUD operations including
 * soft-delete handling and delegation to the persistence outbound port.
 * </p>
 *
 * @author TicketFlow Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EventService implements IEventService {

    private final IEventPersistencePort eventPersistencePort;
    private final IEventApplicationMapper eventApplicationMapper;

    /**
     * {@inheritDoc}
     * <p>
     * Generates a UUID as the event ID server-side before persisting.
     * </p>
     */
    @Override
    public EventResponse create(CreateEventRequest request) {
        String id = UUID.randomUUID().toString();
        log.info("Creating event entry with generated id: {}", id);

        Event event = eventApplicationMapper.toDomain(request);
        event.setId(id);
        Event savedEvent = eventPersistencePort.save(event);

        log.info("Event entry created successfully with id: {}", savedEvent.getId());
        return eventApplicationMapper.toResponse(savedEvent);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Throws {@link EventNotFoundException} if no active event with the given ID exists.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    public EventResponse getById(String id) {
        log.info("Retrieving event entry with id: {}", id);

        Event event = eventPersistencePort.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Event retrieval failed - event with id '{}' not found", id);
                    return new EventNotFoundException(id);
                });

        log.info("Event entry retrieved successfully with id: {}", id);
        return eventApplicationMapper.toResponse(event);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Delegates pagination directly to the persistence port and maps
     * each domain object to a response DTO.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    public Page<EventResponse> getAll(String title, String location, Pageable pageable) {
        log.info("Retrieving all event entries - title: {}, location: {}, page: {}, size: {}",
                title, location, pageable.getPageNumber(), pageable.getPageSize());

        Page<EventResponse> result = eventPersistencePort.findAllByFilters(title, location, pageable)
                .map(eventApplicationMapper::toResponse);

        log.info("Retrieved {} event entries out of {} total", result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Loads the existing event, applies the updates via the application mapper,
     * and persists the changes. Throws {@link EventNotFoundException} if the event
     * does not exist or has been soft-deleted.
     * </p>
     */
    @Override
    public EventResponse update(String id, UpdateEventRequest request) {
        log.info("Updating event entry with id: {}", id);

        Event existingEvent = eventPersistencePort.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Event update failed - event with id '{}' not found", id);
                    return new EventNotFoundException(id);
                });

        eventApplicationMapper.updateDomainFromRequest(request, existingEvent);
        Event savedEvent = eventPersistencePort.update(existingEvent);

        log.info("Event entry updated successfully with id: {}", savedEvent.getId());
        return eventApplicationMapper.toResponse(savedEvent);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Sets the {@code deleted} flag to {@code true} and persists the change.
     * The record remains in the database and is excluded from active queries.
     * Throws {@link EventNotFoundException} if the event does not exist.
     * </p>
     */
    @Override
    public void delete(String id) {
        log.info("Soft-deleting event entry with id: {}", id);

        Event event = eventPersistencePort.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Event soft-delete failed - event with id '{}' not found", id);
                    return new EventNotFoundException(id);
                });

        event.setDeleted(true);
        eventPersistencePort.update(event);

        log.info("Event entry soft-deleted successfully with id: {}", id);
    }

}