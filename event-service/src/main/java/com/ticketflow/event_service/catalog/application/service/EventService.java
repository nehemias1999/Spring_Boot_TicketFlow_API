package com.ticketflow.event_service.catalog.application.service;

import com.ticketflow.event_service.catalog.application.dto.response.EventResponse;
import com.ticketflow.event_service.catalog.application.dto.request.CreateEventRequest;
import com.ticketflow.event_service.catalog.application.dto.request.UpdateEventRequest;
import com.ticketflow.event_service.catalog.application.mapper.IEventApplicationMapper;
import com.ticketflow.event_service.catalog.domain.exception.AccessDeniedException;
import com.ticketflow.event_service.catalog.domain.exception.EventFullException;
import com.ticketflow.event_service.catalog.domain.exception.EventNotFoundException;
import com.ticketflow.event_service.catalog.domain.model.Event;
import com.ticketflow.event_service.catalog.domain.port.in.IEventService;
import com.ticketflow.event_service.catalog.domain.port.out.IEventPersistencePort;
import com.ticketflow.event_service.shared.infrastructure.security.RoleValidator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Application service implementing the {@link IEventService} inbound port.
 *
 * @author TicketFlow Team
 */
@Slf4j
@Service
@Transactional
public class EventService implements IEventService {

    private final IEventPersistencePort eventPersistencePort;
    private final IEventApplicationMapper eventApplicationMapper;
    private final Counter eventsCreatedCounter;
    private final Counter eventsDeletedCounter;

    public EventService(IEventPersistencePort eventPersistencePort,
                        IEventApplicationMapper eventApplicationMapper,
                        MeterRegistry meterRegistry) {
        this.eventPersistencePort = eventPersistencePort;
        this.eventApplicationMapper = eventApplicationMapper;
        this.eventsCreatedCounter = Counter.builder("events.created")
                .description("Total number of events created")
                .register(meterRegistry);
        this.eventsDeletedCounter = Counter.builder("events.deleted")
                .description("Total number of events soft-deleted")
                .register(meterRegistry);
    }

    @Override
    public EventResponse create(CreateEventRequest request, String creatorId, String role) {
        RoleValidator.requireAnyRole(role, "SELLER", "ADMIN");

        String id = UUID.randomUUID().toString();
        log.info("Creating event with generated id: {}", id);

        Event event = eventApplicationMapper.toDomain(request);
        event.setId(id);
        event.setCreatorId("ADMIN".equalsIgnoreCase(role) ? null : creatorId);
        event.setAvailableTickets(request.capacity());

        Event savedEvent = eventPersistencePort.save(event);
        eventsCreatedCounter.increment();
        log.info("Event created successfully with id: {}", savedEvent.getId());
        return eventApplicationMapper.toResponse(savedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse getById(String id) {
        log.info("Retrieving event with id: {}", id);
        Event event = eventPersistencePort.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Event with id '{}' not found", id);
                    return new EventNotFoundException(id);
                });
        return eventApplicationMapper.toResponse(event);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponse> getAll(String title, String location, Pageable pageable) {
        log.info("Retrieving events - title: {}, location: {}, page: {}",
                title, location, pageable.getPageNumber());
        Page<EventResponse> result = eventPersistencePort.findAllByFilters(title, location, pageable)
                .map(eventApplicationMapper::toResponse);
        log.info("Retrieved {} events out of {} total",
                result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    @Override
    public EventResponse update(String id, UpdateEventRequest request, String requestingUserId, String role) {
        RoleValidator.requireAnyRole(role, "SELLER", "ADMIN");

        Event existing = eventPersistencePort.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Event update failed - event '{}' not found", id);
                    return new EventNotFoundException(id);
                });

        if ("SELLER".equalsIgnoreCase(role) && !requestingUserId.equals(existing.getCreatorId())) {
            log.warn("Seller '{}' attempted to update event '{}' owned by '{}'",
                    requestingUserId, id, existing.getCreatorId());
            throw new AccessDeniedException("You can only update your own events");
        }

        eventApplicationMapper.updateDomainFromRequest(request, existing);
        Event saved = eventPersistencePort.update(existing);
        log.info("Event '{}' updated successfully", id);
        return eventApplicationMapper.toResponse(saved);
    }

    @Override
    public void delete(String id, String requestingUserId, String role) {
        RoleValidator.requireAnyRole(role, "SELLER", "ADMIN");

        Event event = eventPersistencePort.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Event delete failed - event '{}' not found", id);
                    return new EventNotFoundException(id);
                });

        if ("SELLER".equalsIgnoreCase(role) && !requestingUserId.equals(event.getCreatorId())) {
            log.warn("Seller '{}' attempted to delete event '{}' owned by '{}'",
                    requestingUserId, id, event.getCreatorId());
            throw new AccessDeniedException("You can only delete your own events");
        }

        event.setDeleted(true);
        eventPersistencePort.update(event);
        eventsDeletedCounter.increment();
        log.info("Event '{}' soft-deleted successfully", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponse> getMyEvents(String creatorId, Pageable pageable) {
        log.info("Retrieving events for creatorId: {}", creatorId);
        return eventPersistencePort.findAllByCreatorIdAndDeletedFalse(creatorId, pageable)
                .map(eventApplicationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getMyEventIds(String creatorId) {
        log.debug("Retrieving event IDs for creatorId: {}", creatorId);
        return eventPersistencePort.findIdsByCreatorIdAndDeletedFalse(creatorId);
    }

    @Override
    public EventResponse decrementAvailableTickets(String eventId) {
        Event event = eventPersistencePort.findByIdAndDeletedFalse(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        if (event.getAvailableTickets() <= 0) {
            log.warn("Event '{}' has no available tickets", eventId);
            throw new EventFullException(eventId);
        }

        event.setAvailableTickets(event.getAvailableTickets() - 1);
        Event saved = eventPersistencePort.update(event);
        log.info("Decremented available tickets for event '{}', now: {}", eventId, saved.getAvailableTickets());
        return eventApplicationMapper.toResponse(saved);
    }

    @Override
    public EventResponse incrementAvailableTickets(String eventId) {
        Event event = eventPersistencePort.findByIdAndDeletedFalse(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        event.setAvailableTickets(event.getAvailableTickets() + 1);
        Event saved = eventPersistencePort.update(event);
        log.info("Incremented available tickets for event '{}', now: {}", eventId, saved.getAvailableTickets());
        return eventApplicationMapper.toResponse(saved);
    }
}
