package com.ticketflow.event_service.catalog.application.service;

import com.ticketflow.event_service.catalog.application.dto.response.EventResponse;
import com.ticketflow.event_service.catalog.application.dto.request.CreateEventRequest;
import com.ticketflow.event_service.catalog.application.dto.request.UpdateEventRequest;
import com.ticketflow.event_service.catalog.application.mapper.IEventApplicationMapper;
import com.ticketflow.event_service.catalog.domain.exception.EventAlreadyExistsException;
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

/**
 * Application service implementing the {@link IEventService} inbound port.
 * <p>
 * Contains all business logic for event CRUD operations including
 * existence validation, soft-delete handling, and delegation to the
 * persistence outbound port.
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
     * Validates that no active event with the same ID exists before persisting.
     * </p>
     */
    @Override
    public EventResponse create(CreateEventRequest request) {
        log.info("Creating event entry with id: {}", request.id());

        if (eventPersistencePort.existsByIdAndDeletedFalse(request.id())) {
            log.warn("Event creation failed - event with id '{}' already exists", request.id());
            throw new EventAlreadyExistsException(request.id());
        }

        Event event = eventApplicationMapper.toDomain(request);
        Event savedEvent = eventPersistencePort.save(event);

        log.info("Event entry created successfully with id: {}", savedEvent.getId());
        return eventApplicationMapper.toResponse(savedEvent);
    }

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

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponse> getAll(Pageable pageable) {
        log.info("Retrieving all event entries - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        Page<EventResponse> result = eventPersistencePort.findAllByDeletedFalse(pageable)
                .map(eventApplicationMapper::toResponse);

        log.info("Retrieved {} event entries out of {} total", result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

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

